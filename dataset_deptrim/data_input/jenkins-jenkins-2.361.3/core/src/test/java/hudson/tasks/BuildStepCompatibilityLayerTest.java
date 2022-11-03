package hudson.tasks;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import java.io.IOException;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mockito;

public class BuildStepCompatibilityLayerTest {

    @Issue("JENKINS-18734")
    @Test
    @SuppressWarnings("deprecation") /* testing deprecated variant */
    public void testPerformExpectAbstractMethodError() {

        FreeStyleBuild mock = Mockito.mock(FreeStyleBuild.class, Mockito.CALLS_REAL_METHODS);
        BuildStepCompatibilityLayer bscl = new BuildStepCompatibilityLayer() {};
        assertThrows(AbstractMethodError.class, () -> bscl.perform(mock, null, null));

    }

    @Issue("JENKINS-18734")
    @Test
    @SuppressWarnings("deprecation")
    public void testPerform() throws InterruptedException, IOException {

        FreeStyleBuild mock = Mockito.mock(FreeStyleBuild.class, Mockito.CALLS_REAL_METHODS);
        BuildStepCompatibilityLayer bscl = new BuildStepCompatibilityLayer() {

            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                return true;
            }
        };
        assertTrue(bscl.perform(mock, null, null));

    }

}
