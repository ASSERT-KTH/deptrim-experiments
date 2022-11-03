package jenkins.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import hudson.ExtensionList;
import hudson.model.UnprotectedRootAction;
import hudson.model.User;
import hudson.util.HttpResponses;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jenkins.security.apitoken.ApiTokenTestHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.HttpResponse;
import org.xml.sax.SAXException;

/**
 * @author Kohsuke Kawaguchi
 */
public class BasicHeaderProcessorTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    private WebClient wc;

    private SpySecurityListener spySecurityListener;

    @Before
    public void prepareListeners() {
        this.spySecurityListener = ExtensionList.lookupSingleton(SpySecurityListener.class);
    }

    /**
     * Tests various ways to send the Basic auth.
     */
    @Test
    public void testVariousWaysToCall() throws Exception {
        ApiTokenTestHelper.enableLegacyBehavior();

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        wc = j.createWebClient();
        User foo = User.getById("foo", true);
        User.getById("bar", true);

        // call without authentication
        makeRequestAndVerify("anonymous");
        spySecurityListener.authenticatedCalls.assertNoNewEvents();
        spySecurityListener.failedToAuthenticateCalls.assertNoNewEvents();

        // call with API token
        wc = j.createWebClient();
        wc.withBasicApiToken("foo");
        makeRequestAndVerify("foo");
        spySecurityListener.authenticatedCalls.assertLastEventIsAndThenRemoveIt(u -> u.getUsername().equals("foo"));

        // call with invalid API token
        wc = j.createWebClient();
        wc.withBasicCredentials("foo", "abcd" + foo.getProperty(ApiTokenProperty.class).getApiToken());
        makeRequestAndFail();
        spySecurityListener.failedToAuthenticateCalls.assertLastEventIsAndThenRemoveIt("foo");

        // call with password
        wc = j.createWebClient();
        wc.withBasicCredentials("foo");
        makeRequestAndVerify("foo");
        spySecurityListener.authenticatedCalls.assertLastEventIsAndThenRemoveIt(u -> u.getUsername().equals("foo"));

        // call with incorrect password
        wc = j.createWebClient();
        wc.withBasicCredentials("foo", "bar");
        makeRequestAndFail();
        spySecurityListener.failedToAuthenticateCalls.assertLastEventIsAndThenRemoveIt("foo");

        wc = j.createWebClient();
        wc.login("bar");
        spySecurityListener.authenticatedCalls.assertLastEventIsAndThenRemoveIt(u -> u.getUsername().equals("bar"));
        spySecurityListener.loggedInCalls.assertLastEventIsAndThenRemoveIt("bar");

        // if the session cookie is valid, then basic header won't be needed
        makeRequestAndVerify("bar");
        spySecurityListener.authenticatedCalls.assertNoNewEvents();
        spySecurityListener.failedToAuthenticateCalls.assertNoNewEvents();

        // if the session cookie is valid, and basic header is set anyway login should not fail either
        wc.withBasicCredentials("bar");
        makeRequestAndVerify("bar");
        spySecurityListener.authenticatedCalls.assertNoNewEvents();
        spySecurityListener.failedToAuthenticateCalls.assertNoNewEvents();

        // but if the password is incorrect, it should fail, instead of silently logging in as the user indicated by session
        wc.withBasicCredentials("foo", "bar");
        makeRequestAndFail();
        spySecurityListener.failedToAuthenticateCalls.assertLastEventIsAndThenRemoveIt("foo");
    }

    private void makeRequestAndFail() throws IOException, SAXException {
        makeRequestWithAuthCodeAndFail(null);
    }

    private void makeRequestAndVerify(String expectedLogin) throws IOException {
        makeRequestWithAuthCodeAndVerify(null, expectedLogin);
    }

    @Test
    public void testAuthHeaderCaseInSensitive() throws Exception {
        ApiTokenTestHelper.enableLegacyBehavior();

        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        User foo = User.getOrCreateByIdOrFullName("foo");
        wc = j.createWebClient();

        String[] basicCandidates = {"Basic", "BASIC", "basic", "bASIC"};

        for (String prefix : basicCandidates) {
            // call with API token
            ApiTokenProperty t = foo.getProperty(ApiTokenProperty.class);
            final String token = t.getApiToken();
            String authCode1 = encode(prefix, "foo:" + token);
            makeRequestWithAuthCodeAndVerify(authCode1, "foo");
            spySecurityListener.authenticatedCalls.assertLastEventIsAndThenRemoveIt(u -> u.getUsername().equals("foo"));

            // call with invalid API token
            String authCode2 = encode(prefix, "foo:abcd" + token);
            makeRequestWithAuthCodeAndFail(authCode2);
            spySecurityListener.failedToAuthenticateCalls.assertLastEventIsAndThenRemoveIt("foo");

            // call with password
            String authCode3 = encode(prefix, "foo:foo");
            makeRequestWithAuthCodeAndVerify(authCode3, "foo");
            spySecurityListener.authenticatedCalls.assertLastEventIsAndThenRemoveIt(u -> u.getUsername().equals("foo"));

            // call with incorrect password
            String authCode4 = encode(prefix, "foo:bar");
            makeRequestWithAuthCodeAndFail(authCode4);
            spySecurityListener.failedToAuthenticateCalls.assertLastEventIsAndThenRemoveIt("foo");
        }
    }

    private String encode(String prefix, String userAndPass) {
        if (userAndPass == null) {
            return null;
        }
        return prefix + " " + Base64.getEncoder().encodeToString(userAndPass.getBytes(StandardCharsets.UTF_8));
    }

    private void makeRequestWithAuthCodeAndVerify(String authCode, String expectedLogin) throws IOException {
        WebRequest req = new WebRequest(new URL(j.getURL(), "test"));
        req.setEncodingType(null);
        if (authCode != null)
            req.setAdditionalHeader("Authorization", authCode);
        Page p = wc.getPage(req);
        assertEquals(expectedLogin, p.getWebResponse().getContentAsString());
    }

    private void makeRequestWithAuthCodeAndFail(String authCode) throws IOException {
        try {
            makeRequestWithAuthCodeAndVerify(authCode, "-");
            fail();
        } catch (FailingHttpStatusCodeException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @TestExtension
    public static class WhoAmI implements UnprotectedRootAction {
        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return "test";
        }

        public HttpResponse doIndex() {
            User u = User.current();
            return HttpResponses.text(u != null ? u.getId() : "anonymous");
        }
    }

    @TestExtension
    public static class SpySecurityListenerImpl extends SpySecurityListener {}
}
