/*
 * The MIT License
 *
 * Copyright (c) 2020, CloudBees, Inc.
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

package lib.form;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElementUtil;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLAnchorElement;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.UnprotectedRootAction;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.sourceforge.htmlunit.corejs.javascript.NativeArray;
import org.jenkinsci.Symbol;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class HeteroListTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    @Issue("SECURITY-2035")
    public void xssPrevented_heteroList_usingDescriptorDisplayName() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();

        RootActionImpl rootAction = ExtensionList.lookupSingleton(RootActionImpl.class);
        TestItemDescribable.DynamicDisplayNameDescriptor dynamic = ExtensionList.lookupSingleton(TestItemDescribable.DynamicDisplayNameDescriptor.class);
        rootAction.descriptorList = List.of(dynamic);

        dynamic.displayName = "Display<strong>Name</strong>";

        HtmlPage page = wc.goTo("root");

        page.executeJavaScript("document.querySelector('.hetero-list-add').click();");
        Object result = page.executeJavaScript("document.querySelector('.yuimenuitem a')").getJavaScriptResult();
        assertThat(result, instanceOf(HTMLAnchorElement.class));
        HTMLAnchorElement menuItem = (HTMLAnchorElement) result;
        String menuItemContent = menuItem.getInnerHTML();
        assertThat(menuItemContent, not(containsString("<")));
    }

    // correspond to the hardening of escapeEntryTitleAndDescription
    @Test
    @Issue("SECURITY-2035")
    public void xssPrevented_usingToolInstallation_withJustDisplayName() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage page = wc.goTo("configureTools/");

        // check the displayName
        Object resultDN = page.executeJavaScript(
                "var settingFields = document.querySelectorAll('.jenkins-form-label');" +
                        "var children = Array.from(settingFields).filter(b => b.textContent.indexOf('XSS:') !== -1)[0].children;" +
                        "Array.from(children).filter(c => c.tagName === 'IMG')"
        ).getJavaScriptResult();
        assertThat(resultDN, instanceOf(NativeArray.class));
        NativeArray resultDNNA = (NativeArray) resultDN;
        assertEquals(0, resultDNNA.size());

        // check the description
        Object resultDesc = page.executeJavaScript(
                "var settingFields = document.querySelectorAll('.jenkins-form-description');" +
                        "var children = Array.from(settingFields).filter(b => b.textContent.indexOf('XSS:') !== -1)[0].children;" +
                        "Array.from(children).filter(c => c.tagName === 'IMG')"
        ).getJavaScriptResult();
        assertThat(resultDesc, instanceOf(NativeArray.class));
        NativeArray resultDescNA = (NativeArray) resultDesc;
        assertEquals(0, resultDescNA.size());
    }

    @Test
    @Issue("SECURITY-2035")
    public void xssPrevented_usingToolInstallation_repeatableAddExisting() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage page = wc.goTo("configureTools/");

        // the existing add button can already trigger an XSS
        Object result = page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.textContent.indexOf('Add XSS') !== -1)[0].innerHTML").getJavaScriptResult();
        assertThat(result, instanceOf(String.class));
        String resultString = (String) result;
        assertThat(resultString, not(containsString("<")));
    }

    // only possible after a partial fix
    @Test
    public void xssPrevented_usingToolInstallation_repeatableAddAfterClick() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage page = wc.goTo("configureTools/");

        Optional<DomElement> addXssButtonRawOptional = page.getElementsByTagName("button").stream().filter(e -> e.getTextContent().contains("Add XSS")).findFirst();
        assertTrue(addXssButtonRawOptional.isPresent());
        assertThat(addXssButtonRawOptional.get(), instanceOf(HtmlButton.class));

        HtmlButton addXssButton = (HtmlButton) addXssButtonRawOptional.get();
        HtmlElementUtil.click(addXssButton);

        // checking only the newly created button (at the top of the panel), hence the [0]
        Object result = page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.textContent.indexOf('Add XSS') !== -1)[0].innerHTML").getJavaScriptResult();
        assertThat(result, instanceOf(String.class));
        String resultString = (String) result;
        assertThat(resultString, not(containsString("<")));
    }

    @Test
    @Issue("SECURITY-2035")
    public void xssPrevented_usingToolInstallation_repeatableAddWithExistingUsingInstallationsButton() throws Exception {
        Xss.DescriptorImpl xssDescriptor = ExtensionList.lookupSingleton(Xss.DescriptorImpl.class);
        xssDescriptor.installations = new Xss[]{ new Xss("name1", "home1", null) };

        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage page = wc.goTo("configureTools/");

        // XSS: [img] installations...
        Object result = page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.textContent.indexOf('XSS:') !== -1)[0].innerHTML").getJavaScriptResult();
        assertThat(result, instanceOf(String.class));
        String resultString = (String) result;
        assertThat(resultString, not(containsString("<")));
    }

    @Test
    @Issue("SECURITY-2035")
    public void xssPrevented_usingToolInstallation_repeatableAddWithExistingAfterOpening() throws Exception {
        Xss.DescriptorImpl xssDescriptor = ExtensionList.lookupSingleton(Xss.DescriptorImpl.class);
        xssDescriptor.installations = new Xss[]{ new Xss("name1", "home1", null) };

        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage page = wc.goTo("configureTools/");

        // Passing the installation button
        page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.textContent.indexOf('XSS:') !== -1)[0].click()");

        // Looking for all the buttons displayed, at this point there is one Add, one Delete and the second Add.
        // Both add are generated through different code.
        // While keeping away the installations... advanced button as it's covered in its own test
        Object result = page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.textContent.indexOf('XSS') !== -1 && b.textContent.indexOf('...') === -1).map(b => b.innerHTML)").getJavaScriptResult();
        assertThat(result, instanceOf(List.class));
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result;
        for (String str : resultList) {
            assertThat(str, not(containsString("<")));
        }

        // "delete" then "add" makes us coming back in scenario covered by xssUsingToolInstallationRepeatableAdd
    }

    @Test
    @Issue("SECURITY-2035")
    public void xssPrevented_usingToolInstallation_repeatableDelete() throws Exception {
        JenkinsRule.WebClient wc = j.createWebClient();

        HtmlPage page = wc.goTo("configureTools/");

        // we could also re-use the same method as used in xssUsingToolInstallationRepeatableAdd
        page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.textContent.indexOf('Add XSS') !== -1)[0].click()");

        Object result = page.executeJavaScript("Array.from(document.querySelectorAll('button')).filter(b => b.title.includes('Delete XSS'))[0].innerHTML").getJavaScriptResult();
        assertThat(result, instanceOf(String.class));
        String resultString = (String) result;
        assertThat(resultString, not(containsString("<img")));
    }

    public static class TestItemDescribable implements Describable<TestItemDescribable> {
        @Override
        public Descriptor<TestItemDescribable> getDescriptor() {
            return ExtensionList.lookupSingleton(DynamicDisplayNameDescriptor.class);
        }

        @TestExtension
        public static class DynamicDisplayNameDescriptor extends Descriptor<TestItemDescribable> {
            public String displayName = "NotYetDefined";

            @NonNull
            @Override
            public String getDisplayName() {
                return displayName;
            }
        }
    }

    @TestExtension
    public static class RootActionImpl implements UnprotectedRootAction {
        public List<Descriptor<?>> descriptorList;

        @Override
        @CheckForNull
        public String getIconFileName() {
            return null;
        }

        @Override
        @CheckForNull
        public String getDisplayName() {
            return null;
        }

        @Override
        @CheckForNull
        public String getUrlName() {
            return "root";
        }
    }

    public static final class Xss extends ToolInstallation {

        public Xss(String name, String home, List<? extends ToolProperty<?>> properties) {
            super(name, home, properties);
        }

        @TestExtension
        @Symbol("tool-xss")
        public static class DescriptorImpl extends ToolDescriptor<Xss> {
            private Xss[] installations = new Xss[0];

            @NonNull
            @Override
            public String getDisplayName() {
                return "XSS: <img src=x onerror=console.warn('" + getClass().getName() + "') />";
            }

            @Override
            public Xss[] getInstallations() {
                return installations;
            }

            @Override
            public void setInstallations(Xss... xsses) {
                this.installations = xsses;
            }

            @Override
            public List<? extends ToolInstaller> getDefaultInstallers() {
                return Collections.emptyList();
            }

            /**
             * Checks if the JAVA_HOME is a valid JAVA_HOME path.
             */
            @Override protected FormValidation checkHomeDirectory(File value) {
                return FormValidation.ok();
            }
        }
    }
}
