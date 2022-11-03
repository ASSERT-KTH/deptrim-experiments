package jenkins.model;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.ModelObject;
import org.jenkinsci.Symbol;

/**
 * Grouping of related {@link GlobalConfiguration}s.
 *
 * <p>
 * To facilitate the separation of the global configuration into multiple pages, tabs, and so on,
 * {@link GlobalConfiguration}s are classified into categories (such as "security", "tools", as well
 * as the catch all "unclassified".) Categories themselves are extensible &mdash; plugins may introduce
 * its own category as well, although that should only happen if you are creating a big enough subsystem.
 *
 * <p>
 * The primary purpose of this is to enable future UIs to split the global configurations to
 * smaller pieces that can be individually looked at and updated.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.494
 * @see GlobalConfiguration
 */
public abstract class GlobalConfigurationCategory implements ExtensionPoint, ModelObject {
    /**
     * One-line plain text message that explains what this category is about.
     * This can be used in the UI to help the user pick the right category.
     *
     * The text should be longer than {@link #getDisplayName()}
     */
    public abstract String getShortDescription();

    /**
     * Returns all the registered {@link GlobalConfiguration} descriptors.
     */
    public static ExtensionList<GlobalConfigurationCategory> all() {
        return ExtensionList.lookup(GlobalConfigurationCategory.class);
    }

    public static @NonNull <T extends GlobalConfigurationCategory> T get(Class<T> type) {
        T category = all().get(type);
        if (category == null) {
            throw new AssertionError("Category not found. It seems the " + type + " is not annotated with @Extension and so not registered");
        }
        return category;
    }

    /**
     * This category represents the catch-all I-dont-know-what-category-it-is instance,
     * used for those {@link GlobalConfiguration}s that don't really deserve/need a separate
     * category.
     *
     * Also used for backward compatibility. All {@link GlobalConfiguration}s without
     * explicit category gets this as the category.
     *
     * In the current UI, this corresponds to the /configure link.
     */
    @Extension @Symbol("unclassified")
    public static class Unclassified extends GlobalConfigurationCategory {
        @Override
        public String getShortDescription() {
            return jenkins.management.Messages.ConfigureLink_Description();
        }

        @Override
        public String getDisplayName() {
            return jenkins.management.Messages.ConfigureLink_DisplayName();
        }
    }

    /**
     * Security related configurations.
     */
    @Extension @Symbol("security")
    public static class Security extends GlobalConfigurationCategory {
        @Override
        public String getShortDescription() {
            return hudson.security.Messages.GlobalSecurityConfiguration_Description();
        }

        @Override
        public String getDisplayName() {
            return hudson.security.Messages.GlobalSecurityConfiguration_DisplayName();
        }
    }

}
