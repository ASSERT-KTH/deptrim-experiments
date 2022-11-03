/*
 * The MIT License
 *
 * Copyright 2012 Jesse Glick.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import hudson.PluginWrapper;
import hudson.model.UpdateSite.Data;
import hudson.util.FormValidation;
import hudson.util.PersistedList;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jenkins.model.Jenkins;
import jenkins.security.UpdateSiteWarningsConfiguration;
import jenkins.security.UpdateSiteWarningsMonitor;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class UpdateSiteTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    private final String RELATIVE_BASE = "/_relative/";
    private Server server;
    private URL baseUrl;

    private String getResource(String resourceName) throws IOException {
        try {
            URL url = UpdateSiteTest.class.getResource(resourceName);
            return url != null ? Files.readString(Paths.get(url.toURI()), StandardCharsets.UTF_8) : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Startup a web server to access resources via HTTP.
     */
    @Before
    public void setUpWebServer() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
                if (target.startsWith(RELATIVE_BASE)) {
                    target = target.substring(RELATIVE_BASE.length());
                }
                String responseBody = getResource(target);
                if (responseBody != null) {
                    baseRequest.setHandled(true);
                    response.setContentType("text/plain; charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
                }
            }
        });
        server.start();
        baseUrl = new URL("http", "localhost", connector.getLocalPort(), RELATIVE_BASE);
    }

    @After
    public void shutdownWebserver() throws Exception {
        server.stop();
    }

    @Test public void relativeURLs() throws Exception {
        URL url = new URL(baseUrl, "/plugins/htmlpublisher-update-center.json");
        UpdateSite site = new UpdateSite(UpdateCenter.ID_DEFAULT, url.toString());
        overrideUpdateSite(site);
        assertEquals(FormValidation.ok(), site.updateDirectly(false).get());
        Data data = site.getData();
        assertNotNull(data);
        assertEquals(new URL(url, "jenkins.war").toString(), data.core.url);
        assertEquals(new HashSet<>(Arrays.asList("htmlpublisher", "dummy")), data.plugins.keySet());
        assertEquals(new URL(url, "htmlpublisher.jpi").toString(), data.plugins.get("htmlpublisher").url);
        assertEquals("http://nowhere.net/dummy.hpi", data.plugins.get("dummy").url);

        UpdateSite.Plugin htmlPublisherPlugin = data.plugins.get("htmlpublisher");
        assertEquals("Wrong name of plugin found", "HTML Publisher", htmlPublisherPlugin.getDisplayName());
    }

    @Test public void wikiUrlFromSingleSite() throws Exception {
        UpdateSite site = getUpdateSite("/plugins/htmlpublisher-update-center.json");
        overrideUpdateSite(site);
        PluginWrapper wrapper = buildPluginWrapper("dummy", "https://wiki.jenkins.io/display/JENKINS/dummy");
        assertEquals("https://plugins.jenkins.io/dummy", wrapper.getUrl());
    }

    @Test public void wikiUrlFromMoreSites() throws Exception {
        UpdateSite site = getUpdateSite("/plugins/htmlpublisher-update-center.json");
        UpdateSite alternativeSite = getUpdateSite("/plugins/alternative-update-center.json", "alternative");
        overrideUpdateSite(site, alternativeSite);
        // sites use different Wiki URL for dummy -> use URL from manifest
        PluginWrapper wrapper = buildPluginWrapper("dummy", "https://wiki.jenkins.io/display/JENKINS/dummy");
        assertEquals("https://wiki.jenkins.io/display/JENKINS/dummy", wrapper.getUrl());
        // sites use the same Wiki URL for HTML Publisher -> use it
        wrapper = buildPluginWrapper("htmlpublisher", "https://plugins.jenkins.io/htmlpublisher");
        assertEquals("https://plugins.jenkins.io/htmlpublisher", wrapper.getUrl());
        // only one site has it
        wrapper = buildPluginWrapper("foo", "https://wiki.jenkins.io/display/JENKINS/foo");
        assertEquals("https://plugins.jenkins.io/foo", wrapper.getUrl());
    }

    @Test public void updateDirectlyWithJson() throws Exception {
        UpdateSite us = new UpdateSite("default", new URL(baseUrl, "update-center.json").toExternalForm());
        assertNull(us.getPlugin("AdaptivePlugin"));
        assertEquals(FormValidation.ok(), us.updateDirectly(/* TODO the certificate is now expired, and downloading a fresh copy did not seem to help */false).get());
        assertNotNull(us.getPlugin("AdaptivePlugin"));
    }

    @Test public void lackOfDataDoesNotFailWarningsCode() {
        assertNull("plugin data is not present", j.jenkins.getUpdateCenter().getSite("default").getData());

        // nothing breaking?
        j.jenkins.getExtensionList(UpdateSiteWarningsMonitor.class).get(0).getActivePluginWarningsByPlugin();
        j.jenkins.getExtensionList(UpdateSiteWarningsMonitor.class).get(0).getActiveCoreWarnings();
        j.jenkins.getExtensionList(UpdateSiteWarningsConfiguration.class).get(0).getAllWarnings();
    }

    @Test public void incompleteWarningsJson() throws Exception {
        UpdateSite site = getUpdateSite("/plugins/warnings-update-center-malformed.json");
        overrideUpdateSite(site);
        assertEquals("number of warnings", 7, site.getData().getWarnings().size());
        assertNotEquals("plugin data is present", Collections.emptyMap(), site.getData().plugins);
    }

    @Test public void getAvailables() throws Exception {
        UpdateSite site = getUpdateSite("/plugins/available-update-center.json");
        List<UpdateSite.Plugin> available = site.getAvailables();
        assertEquals("ALowTitle", available.get(0).getDisplayName());
        assertEquals("TheHighTitle", available.get(1).getDisplayName());
    }

    private UpdateSite getUpdateSite(String path) throws Exception {
        return getUpdateSite(path, UpdateCenter.ID_DEFAULT);
    }

    private UpdateSite getUpdateSite(String path, String id) throws Exception {
        URL url = new URL(baseUrl, path);
        UpdateSite site = new UpdateSite(id, url.toString());
        assertEquals(FormValidation.ok(), site.updateDirectly(false).get());
        return site;
    }

    private void overrideUpdateSite(UpdateSite... overrideSites) {
        PersistedList<UpdateSite> sites = j.jenkins.getUpdateCenter().getSites();
        sites.clear();
        sites.addAll(Arrays.asList(overrideSites));
    }

    private PluginWrapper buildPluginWrapper(String name, String wikiUrl) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(new Attributes.Name("Short-Name"), name);
        attributes.put(new Attributes.Name("Plugin-Version"), "1.0.0");
        attributes.put(new Attributes.Name("Url"), wikiUrl);
        return new PluginWrapper(
                Jenkins.get().getPluginManager(),
                new File("/tmp/" + name + ".jpi"),
                manifest,
                null,
                null,
                new File("/tmp/" + name + ".jpi.disabled"),
                null,
                new ArrayList<>()
        );
    }
}
