package jenkins.install;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import java.util.List;
import javax.inject.Provider;

/**
 * Allows plugging in to the lifecycle when determining InstallState
 * from {@link InstallUtil#getNextInstallState(InstallState)}
 */
public abstract class InstallStateFilter implements ExtensionPoint {
    /**
     * Determine the current or next install state, proceed with `return proceed.next()`
     */
    public abstract InstallState getNextInstallState(InstallState current, Provider<InstallState> proceed);

    /**
     * Get all the InstallStateFilters, in extension order
     */
    public static List<InstallStateFilter> all() {
        return ExtensionList.lookup(InstallStateFilter.class);
    }
}
