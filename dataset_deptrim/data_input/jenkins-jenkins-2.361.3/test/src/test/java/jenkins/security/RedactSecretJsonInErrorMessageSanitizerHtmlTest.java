/*
 * The MIT License
 *
 * Copyright (c) 2018 CloudBees, Inc.
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

package jenkins.security;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.RootAction;
import hudson.util.Secret;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

@Restricted(NoExternalUse.class)
public class RedactSecretJsonInErrorMessageSanitizerHtmlTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public LoggerRule logging = new LoggerRule();

    @Test
    @Issue("SECURITY-765")
    public void passwordsAreRedacted_andOtherStayTheSame() throws Exception {
        j.jenkins.setCrumbIssuer(null);
        TestPassword testPassword = j.jenkins.getExtensionList(RootAction.class).get(TestPassword.class);

        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage page = wc.goTo("test");

        String textSimple = "plain-1";
        String pwdSimple = "secret-1";
        ((HtmlInput) page.getElementById("text-simple")).setValueAttribute(textSimple);
        ((HtmlInput) page.getElementById("pwd-simple")).setValueAttribute(pwdSimple);

        String textLevelOne = "plain-2";
        String pwdLevelOneA = "secret-2";
        ((HtmlInput) page.getElementById("text-level-one")).setValueAttribute(textLevelOne);
        ((HtmlInput) page.getElementById("pwd-level-one-a")).setValueAttribute(pwdLevelOneA);

        HtmlForm form = page.getFormByName("config");
        Page formSubmitPage = j.submit(form);
        assertThat(formSubmitPage.getWebResponse().getStatusCode(), equalTo(200));

        JSONObject rawJson = testPassword.lastJsonReceived;
        String rawJsonToString = rawJson.toString();
        assertThat(rawJsonToString, containsString(textSimple));
        assertThat(rawJsonToString, containsString(pwdSimple));
        assertThat(rawJsonToString, containsString(textLevelOne));
        assertThat(rawJsonToString, containsString(pwdLevelOneA));

        assertThat(rawJson.getString(RedactSecretJsonInErrorMessageSanitizer.REDACT_KEY), equalTo("pwd-simple"));
        assertThat(
                rawJson.getJSONObject("sub-one").getJSONArray(RedactSecretJsonInErrorMessageSanitizer.REDACT_KEY),
                allOf(
                        hasItem("pwd-level-one-a"),
                        hasItem("pwd-level-one-b")
                )
        );

        String pwdLevelOneB = "pre-set secret"; // set in Jelly
        JSONObject redactedJson = RedactSecretJsonInErrorMessageSanitizer.INSTANCE.sanitize(rawJson);
        String redactedJsonToString = redactedJson.toString();
        assertThat(redactedJsonToString, containsString(textSimple));
        assertThat(redactedJsonToString, not(containsString(pwdSimple)));
        assertThat(redactedJsonToString, containsString(textLevelOne));
        assertThat(redactedJsonToString, not(containsString(pwdLevelOneA)));
        assertThat(redactedJsonToString, not(containsString(pwdLevelOneB)));
        assertThat(redactedJsonToString, containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE));
    }

    @TestExtension("passwordsAreRedacted_andOtherStayTheSame")
    public static class TestPassword implements RootAction {

        public JSONObject lastJsonReceived;

        public void doSubmitTest(StaplerRequest req, StaplerResponse res) throws Exception {
            lastJsonReceived = req.getSubmittedForm();

            res.setStatus(200);
        }

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
    }

    @Test
    @Issue("SECURITY-765")
    public void checkSanitizationIsApplied_inDescriptor() throws Exception {
        logging.record("", Level.WARNING).capture(100);

        j.jenkins.setCrumbIssuer(null);

        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.goTo("testDescribable");
        String secret = "s3cr3t";
        ((HtmlInput) page.getElementById("password")).setValueAttribute(secret);

        HtmlForm form = page.getFormByName("config");
        Page formSubmitPage = j.submit(form);
        assertThat(formSubmitPage.getWebResponse().getContentAsString(), allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));

        // check the system log also
        Throwable thrown = logging.getRecords().stream().filter(r -> r.getMessage().contains("Error while serving")).findAny().get().getThrown();
        // the exception from Descriptor
        assertThat(thrown.getCause().getMessage(), allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));

        // the exception from RequestImpl
        assertThat(thrown.getCause().getCause().getMessage(), allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));

        StringWriter buffer = new StringWriter();
        thrown.printStackTrace(new PrintWriter(buffer));
        String fullStack = buffer.getBuffer().toString();
        assertThat(fullStack, allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));
    }

    @Test
    @Issue("SECURITY-765")
    public void checkSanitizationIsApplied_inStapler() throws Exception {
        logging.record("", Level.WARNING).capture(100);

        j.jenkins.setCrumbIssuer(null);

        JenkinsRule.WebClient wc = j.createWebClient();
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = wc.goTo("testStapler");
        String secret = "s3cr3t";
        ((HtmlInput) page.getElementById("password")).setValueAttribute(secret);

        HtmlForm form = page.getFormByName("config");
        Page formSubmitPage = j.submit(form);
        assertThat(formSubmitPage.getWebResponse().getContentAsString(), allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));

        // check the system log also
        Throwable thrown = logging.getRecords().stream().filter(r -> r.getMessage().contains("Error while serving")).findAny().get().getThrown();
        // the exception from RequestImpl
        assertThat(thrown.getCause().getMessage(), allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));

        StringWriter buffer = new StringWriter();
        thrown.printStackTrace(new PrintWriter(buffer));
        String fullStack = buffer.getBuffer().toString();
        assertThat(fullStack, allOf(
                containsString(RedactSecretJsonInErrorMessageSanitizer.REDACT_VALUE),
                not(containsString(secret))
        ));
    }

    public static class TestDescribable implements Describable<TestDescribable> {

        @DataBoundConstructor
        public TestDescribable(Secret password) {
            throw new IllegalArgumentException("Try to steal my password");
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return Jenkins.get().getDescriptorByType(TestDescribable.DescriptorImpl.class);
        }

        @TestExtension({
                "checkSanitizationIsApplied_inStapler",
                "checkSanitizationIsApplied_inDescriptor"
        })
        public static final class DescriptorImpl extends Descriptor<TestDescribable> {

        }
    }

    @TestExtension("checkSanitizationIsApplied_inDescriptor")
    public static class TestDescribablePage implements RootAction {

        public TestDescribable testDescribable;

        @POST
        public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws Exception {
            Jenkins.get().getDescriptorOrDie(TestDescribable.class).newInstance(req, req.getSubmittedForm());
        }

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
            return "testDescribable";
        }
    }

    @TestExtension("checkSanitizationIsApplied_inStapler")
    public static class TestStaplerPage implements RootAction {

        public TestDescribable testDescribable;

        @POST
        public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws Exception {
            req.bindJSON(TestDescribable.class, req.getSubmittedForm());
        }

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
            return "testStapler";
        }
    }
}
