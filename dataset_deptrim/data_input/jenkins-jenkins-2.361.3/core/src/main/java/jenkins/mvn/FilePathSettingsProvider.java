package jenkins.mvn;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 * @author Dominik Bartholdi (imod)
 * @since 1.491
 */
public class FilePathSettingsProvider extends SettingsProvider {

    private final String path;

    @DataBoundConstructor
    public FilePathSettingsProvider(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public FilePath supplySettings(AbstractBuild<?, ?> build, TaskListener listener) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        try {
            return SettingsPathHelper.getSettings(build, listener, getPath());
        } catch (Exception e) {
            throw new IllegalStateException("failed to prepare settings.xml", e);
        }
    }

    @Extension(ordinal = 10) @Symbol("filePath")
    public static class DescriptorImpl extends SettingsProviderDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.FilePathSettingsProvider_DisplayName();
        }
    }
}
