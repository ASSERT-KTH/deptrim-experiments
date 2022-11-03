/*
 * The MIT License
 *
 * Copyright (c) 2012, CloudBees, Intl., Nicolas De loof
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

package jenkins.management;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.model.UpdateCenter;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension(ordinal = Integer.MAX_VALUE - 400) @Symbol("plugins")
public class PluginsLink extends ManagementLink {

    @Override
    public String getIconFileName() {
        return "plugin.svg";
    }

    @Override
    public String getDisplayName() {
        return Messages.PluginsLink_DisplayName();
    }

    @Override
    public String getDescription() {
        return Messages.PluginsLink_Description();
    }

    @Override
    public String getUrlName() {
        return "pluginManager";
    }

    @NonNull
    @Override
    public Permission getRequiredPermission() {
        return Jenkins.SYSTEM_READ;
    }

    @NonNull
    @Override
    public Category getCategory() {
        return Category.CONFIGURATION;
    }

    @Restricted(NoExternalUse.class)
    public int getUpdateCount() {
        final UpdateCenter updateCenter = Jenkins.get().getUpdateCenter();
        if (!updateCenter.isSiteDataReady()) {
            // Do not display message during this page load, but possibly later.
            return 0;
        }
        return updateCenter.getUpdates().size();
    }
}
