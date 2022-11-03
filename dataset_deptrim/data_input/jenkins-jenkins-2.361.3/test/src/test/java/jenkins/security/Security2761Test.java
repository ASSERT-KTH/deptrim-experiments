package jenkins.security;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.InvisibleAction;
import hudson.model.UnprotectedRootAction;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class Security2761Test {
    public static final String ACTION_URL = "security2761";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Issue("SECURITY-2761")
    @Test
    public void symbolIconAltIsEscaped() throws Exception {
        final AtomicBoolean alerted = new AtomicBoolean(false);

        JenkinsRule.WebClient wc = j.createWebClient();
        wc.setAlertHandler((page, s) -> alerted.set(true));
        HtmlPage page = wc.getPage(new URL(wc.getContextPath() + ACTION_URL));
        String responseContent = page.getWebResponse().getContentAsString();
        wc.waitForBackgroundJavaScript(5000);

        assertThat(responseContent, not(containsString("<img src=x")));
        assertThat(responseContent, containsString("<span class=\"jenkins-visually-hidden\">&lt;img src=x"));
        assertFalse("no alert expected", alerted.get());
    }

    @TestExtension
    public static class ViewHolder extends InvisibleAction implements UnprotectedRootAction {
        @Override
        public String getUrlName() {
            return ACTION_URL;
        }

        public String getTitle() {
            return "<img src=x onerror=alert(1)>";
        }
    }
}
