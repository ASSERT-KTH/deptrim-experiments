package hudson.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import org.junit.Test;

public class EnvironmentContributingActionTest {
    static class OverrideRun extends InvisibleAction implements EnvironmentContributingAction {
        private boolean wasCalled = false;

        @Override
        public void buildEnvironment(@NonNull Run<?, ?> run, @NonNull EnvVars env) {
            wasCalled = true;
        }

        boolean wasNewMethodCalled() {
            return wasCalled;
        }
    }

    static class OverrideAbstractBuild extends InvisibleAction implements EnvironmentContributingAction {
        private boolean wasCalled = false;

        @Override
        @SuppressWarnings("deprecation")
        public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
            wasCalled = true;
        }

        boolean wasDeprecatedMethodCalled() {
            return wasCalled;
        }
    }

    static class OverrideBoth extends InvisibleAction implements EnvironmentContributingAction {
        private boolean wasCalledAbstractBuild = false;
        private boolean wasCalledRun = false;

        @SuppressWarnings("deprecation")
        @Override
        public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
            wasCalledAbstractBuild = true;
        }

        @Override
        public void buildEnvironment(@NonNull Run<?, ?> run, @NonNull EnvVars env) {
            wasCalledRun = true;
        }

        boolean wasDeprecatedMethodCalled() {
            return wasCalledAbstractBuild;
        }

        boolean wasRunCalled() {
            return wasCalledRun;
        }
    }

    private final EnvVars envVars = mock(EnvVars.class);

    @Test
    public void testOverrideRunMethodAndCallNewMethod() {
        Run<?, ?> run = mock(Run.class);

        OverrideRun overrideRun = new OverrideRun();
        overrideRun.buildEnvironment(run, envVars);

        assertTrue(overrideRun.wasNewMethodCalled());
    }

    /**
     * If only non-deprecated method was overridden it would be executed even if someone would call deprecated method.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testOverrideRunMethodAndCallDeprecatedMethod() {
        AbstractBuild<?, ?> abstractBuild = mock(AbstractBuild.class);
        when(abstractBuild.getBuiltOn()).thenReturn(mock(Node.class));

        OverrideRun overrideRun = new OverrideRun();
        overrideRun.buildEnvVars(abstractBuild, envVars);

        assertTrue(overrideRun.wasNewMethodCalled());
    }

    /**
     * {@link AbstractBuild} should work as before.
     */
    @Test
    public void testOverrideAbstractBuildAndCallNewMethodWithAbstractBuild() {
        AbstractBuild<?, ?> abstractBuild = mock(AbstractBuild.class);

        OverrideAbstractBuild action = new OverrideAbstractBuild();
        action.buildEnvironment(abstractBuild, envVars);

        assertTrue(action.wasDeprecatedMethodCalled());
    }

    /**
     * {@link Run} should not execute method that was overridden for {@link AbstractBuild}.
     */
    @Test
    public void testOverrideAbstractBuildAndCallNewMethodWithRun() {
        Run<?, ?> run = mock(Run.class);

        OverrideAbstractBuild action = new OverrideAbstractBuild();
        action.buildEnvironment(run, envVars);

        assertFalse(action.wasDeprecatedMethodCalled());
    }

    /**
     * If someone wants to use overridden deprecated method, it would still work.
     */
    @Test
    public void testOverrideAbstractBuildAndCallDeprecatedMethod() {
        AbstractBuild<?, ?> abstractBuild = mock(AbstractBuild.class);

        OverrideAbstractBuild overrideRun = new OverrideAbstractBuild();
        overrideRun.buildEnvVars(abstractBuild, envVars);

        assertTrue(overrideRun.wasDeprecatedMethodCalled());
    }

    @Test
    public void testOverrideBothAndCallNewMethod() {
        Run<?, ?> run = mock(Run.class);

        OverrideBoth overrideRun = new OverrideBoth();
        overrideRun.buildEnvironment(run, envVars);

        assertTrue(overrideRun.wasRunCalled());
    }

    @Test
    public void testOverrideBothAndCallDeprecatedMethod() {
        AbstractBuild<?, ?> abstractBuild = mock(AbstractBuild.class);

        OverrideBoth overrideRun = new OverrideBoth();
        overrideRun.buildEnvVars(abstractBuild, envVars);

        assertTrue(overrideRun.wasDeprecatedMethodCalled());
    }
}
