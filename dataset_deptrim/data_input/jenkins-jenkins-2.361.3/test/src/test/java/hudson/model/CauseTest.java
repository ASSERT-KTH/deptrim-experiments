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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.XmlFile;
import hudson.tasks.BuildTrigger;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.recipes.LocalData;
import org.xml.sax.SAXException;

public class CauseTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Issue("JENKINS-14814")
    @Test public void deeplyNestedCauses() throws Exception {
        FreeStyleProject a = j.createFreeStyleProject("a");
        FreeStyleProject b = j.createFreeStyleProject("b");
        Run<?, ?> early = null;
        Run<?, ?> last = null;
        for (int i = 1; i <= 15; i++) {
            last = b.scheduleBuild2(0, new Cause.UpstreamCause((Run<?, ?>) a.scheduleBuild2(0, last == null ? null : new Cause.UpstreamCause(last)).get())).get();
            if (i == 5) {
                early = last;
            }
        }
        String buildXml = new XmlFile(Run.XSTREAM, new File(early.getRootDir(), "build.xml")).asString();
        assertTrue("keeps full history:\n" + buildXml, buildXml.contains("<upstreamBuild>1</upstreamBuild>"));
        buildXml = new XmlFile(Run.XSTREAM, new File(last.getRootDir(), "build.xml")).asString();
        assertFalse("too big:\n" + buildXml, buildXml.contains("<upstreamBuild>1</upstreamBuild>"));
    }

    @Issue("JENKINS-15747")
    @Test public void broadlyNestedCauses() throws Exception {
        FreeStyleProject a = j.createFreeStyleProject("a");
        FreeStyleProject b = j.createFreeStyleProject("b");
        FreeStyleProject c = j.createFreeStyleProject("c");
        Run<?, ?> last = null;
        for (int i = 1; i <= 10; i++) {
            Cause cause = last == null ? null : new Cause.UpstreamCause(last);
            Future<? extends Run<?, ?>> next1 = a.scheduleBuild2(0, cause);
            a.scheduleBuild2(0, cause);
            cause = new Cause.UpstreamCause(next1.get());
            Future<? extends Run<?, ?>> next2 = b.scheduleBuild2(0, cause);
            b.scheduleBuild2(0, cause);
            cause = new Cause.UpstreamCause(next2.get());
            Future<? extends Run<?, ?>> next3 = c.scheduleBuild2(0, cause);
            c.scheduleBuild2(0, cause);
            last = next3.get();
        }
        int count = new XmlFile(Run.XSTREAM, new File(last.getRootDir(), "build.xml")).asString().split(Pattern.quote("<hudson.model.Cause_-UpstreamCause")).length;
        assertFalse("too big at " + count, count > 100);
        //j.interactiveBreak();
    }


    @Issue("JENKINS-48467")
    @Test public void userIdCausePrintTest() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TaskListener listener = new StreamTaskListener(baos, Charset.defaultCharset());

        //null userId - print unknown or anonymous
        Cause causeA = new Cause.UserIdCause(null);
        causeA.print(listener);

        assertEquals("Started by user unknown or anonymous", baos.toString(Charset.defaultCharset()).trim());
        baos.reset();

        //SYSTEM userid  - getDisplayName() should be SYSTEM
        Cause causeB = new Cause.UserIdCause();
        causeB.print(listener);

        assertThat(baos.toString(Charset.defaultCharset()), containsString("SYSTEM"));
        baos.reset();

        //unknown userid - print unknown or anonymous
        Cause causeC = new Cause.UserIdCause("abc123");
        causeC.print(listener);

        assertEquals("Started by user unknown or anonymous", baos.toString(Charset.defaultCharset()).trim());
        baos.reset();

        //More or less standard operation
        //user userid  - getDisplayName() should be foo
        User user = User.getById("foo", true);
        Cause causeD = new Cause.UserIdCause(user.getId());
        causeD.print(listener);

        assertThat(baos.toString(Charset.defaultCharset()), containsString(user.getDisplayName()));
        baos.reset();
    }

    @Test
    @Issue("SECURITY-1960")
    @LocalData
    public void xssInRemoteCause() throws IOException, SAXException {
        final Item item = j.jenkins.getItemByFullName("fs");
        Assert.assertTrue(item instanceof FreeStyleProject);
        FreeStyleProject fs = (FreeStyleProject) item;
        final FreeStyleBuild build = fs.getBuildByNumber(1);

        final JenkinsRule.WebClient wc = j.createWebClient();
        final String content = wc.getPage(build).getWebResponse().getContentAsString();
        Assert.assertFalse(content.contains("Started by remote host <img"));
        Assert.assertTrue(content.contains("Started by remote host &lt;img"));
    }

    @Test
    @Issue("SECURITY-1901")
    public void preventXssInUpstreamDisplayName() throws Exception {
        j.jenkins.setQuietPeriod(0);
        FreeStyleProject up = j.createFreeStyleProject("up");
        up.setDisplayName("Up<script>alert(123)</script>Project");

        FreeStyleProject down = j.createFreeStyleProject("down");

        up.getPublishersList().add(new BuildTrigger(down.getFullName(), false));

        j.jenkins.rebuildDependencyGraph();

        j.buildAndAssertSuccess(up);

        FreeStyleBuild downBuild = this.waitForDownBuild(down);

        ensureXssIsPrevented(downBuild);
    }

    @Test
    @Issue("SECURITY-1901")
    public void preventXssInUpstreamDisplayName_deleted() throws Exception {
        j.jenkins.setQuietPeriod(0);
        FreeStyleProject up = j.createFreeStyleProject("up");
        up.setDisplayName("Up<script>alert(123)</script>Project");

        FreeStyleProject down = j.createFreeStyleProject("down");

        up.getPublishersList().add(new BuildTrigger(down.getFullName(), false));

        j.jenkins.rebuildDependencyGraph();

        FreeStyleBuild upBuild = j.buildAndAssertSuccess(up);

        FreeStyleBuild downBuild = this.waitForDownBuild(down);

        // that will display a different part
        upBuild.delete();

        ensureXssIsPrevented(downBuild);
    }

    @Test
    @Issue("SECURITY-1901")
    public void preventXssInUpstreamShortDescription() throws Exception {
        FullNameChangingProject up = j.createProject(FullNameChangingProject.class, "up");

        FreeStyleProject down = j.createFreeStyleProject("down");

        CustomBuild upBuild = j.buildAndAssertSuccess(up);

        up.setVirtualName("Up<script>alert(123)</script>Project");
        j.assertBuildStatusSuccess(down.scheduleBuild2(0, new Cause.UpstreamCause(upBuild)));
        up.setVirtualName(null);

        FreeStyleBuild downBuild = this.waitForDownBuild(down);

        ensureXssIsPrevented(downBuild);
    }

    private void ensureXssIsPrevented(FreeStyleBuild downBuild) throws Exception {
        AtomicBoolean alertCalled = new AtomicBoolean(false);

        JenkinsRule.WebClient wc = j.createWebClient();
        wc.setAlertHandler((page, s) -> alertCalled.set(true));
        wc.goTo(downBuild.getUrl());

        assertFalse("XSS not prevented", alertCalled.get());
    }

    private <B extends Build<?, B>> B waitForDownBuild(Project<?, B> down) throws Exception {
        j.waitUntilNoActivity();
        B result = down.getBuilds().getLastBuild();

        return result;
    }


    @Test
    @Issue("SECURITY-2452")
    public void basicCauseIsSafe() throws Exception {
        final FreeStyleProject fs = j.createFreeStyleProject();
        {
            final FreeStyleBuild build = j.waitForCompletion(fs.scheduleBuild2(0, new SimpleCause("safe")).get());

            final JenkinsRule.WebClient wc = j.createWebClient();
            final String content = wc.getPage(build).getWebResponse().getContentAsString();
            Assert.assertTrue(content.contains("Simple cause: safe"));
        }
        {
            final FreeStyleBuild build = j.waitForCompletion(fs.scheduleBuild2(0, new SimpleCause("<img src=x onerror=alert(1)>")).get());

            final JenkinsRule.WebClient wc = j.createWebClient();
            final String content = wc.getPage(build).getWebResponse().getContentAsString();
            Assert.assertFalse(content.contains("Simple cause: <img"));
            Assert.assertTrue(content.contains("Simple cause: &lt;img"));
        }
    }

    public static class SimpleCause extends Cause {
        private final String description;

        public SimpleCause(String description) {
            this.description = description;
        }

        @Override
        public String getShortDescription() {
            return "Simple cause: " + description;
        }
    }

    public static class CustomBuild extends Build<FullNameChangingProject, CustomBuild> {
        public CustomBuild(FullNameChangingProject job) throws IOException {
            super(job);
        }
    }

    static class FullNameChangingProject extends Project<FullNameChangingProject, CustomBuild> implements TopLevelItem {
        private volatile String virtualName;

        FullNameChangingProject(ItemGroup parent, String name) {
            super(parent, name);
        }

        public void setVirtualName(String virtualName) {
            this.virtualName = virtualName;
        }

        @Override
        public String getName() {
            if (virtualName != null) {
                return virtualName;
            } else {
                return super.getName();
            }
        }

        @Override
        protected Class<CustomBuild> getBuildClass() {
            return CustomBuild.class;
        }

        @Override
        public TopLevelItemDescriptor getDescriptor() {
            return (FreeStyleProject.DescriptorImpl) Jenkins.get().getDescriptorOrDie(getClass());
        }

        @TestExtension
        public static class DescriptorImpl extends AbstractProjectDescriptor {

            @Override
            public FullNameChangingProject newInstance(ItemGroup parent, String name) {
                return new FullNameChangingProject(parent, name);
            }
        }
    }
}
