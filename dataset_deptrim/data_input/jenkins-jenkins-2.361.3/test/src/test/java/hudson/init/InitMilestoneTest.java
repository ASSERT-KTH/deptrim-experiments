package hudson.init;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class InitMilestoneTest {

    @Rule
    public JenkinsRule r  = new JenkinsRule();

    @Test
    public void testInitMilestones() {

        List<InitMilestone> attained = r.jenkins.getExtensionList(Initializers.class).get(0).getAttained();

        assertEquals(InitMilestone.EXTENSIONS_AUGMENTED, attained.get(0));
        assertEquals(InitMilestone.SYSTEM_CONFIG_LOADED, attained.get(1));
        assertEquals(InitMilestone.SYSTEM_CONFIG_ADAPTED, attained.get(2));
        assertEquals(InitMilestone.JOB_LOADED, attained.get(3));
        assertEquals(InitMilestone.JOB_CONFIG_ADAPTED, attained.get(4));
    }

    // Using @Initializer in static methods to check all the InitMilestones are loaded in all tests instances and make them fail,
    // so using a TestExtension and checking only the InitMilestone after EXTENSION_AUGMENTED
    @TestExtension("testInitMilestones")
    public static class Initializers {
        private int order = 0;
        private List<InitMilestone> attained = new ArrayList<>();

        @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
        public void extensionsAugmented() {
            attained.add(order++, InitMilestone.EXTENSIONS_AUGMENTED);
        }

        @Initializer(after = InitMilestone.SYSTEM_CONFIG_LOADED)
        public void pluginsSystemConfigLoaded() {
            attained.add(order++, InitMilestone.SYSTEM_CONFIG_LOADED);
        }

        @Initializer(after = InitMilestone.SYSTEM_CONFIG_ADAPTED)
        public void pluginsSystemConfigAdapted() {
            attained.add(order++, InitMilestone.SYSTEM_CONFIG_ADAPTED);
        }

        @Initializer(after = InitMilestone.JOB_LOADED)
        public void jobLoaded() {
            attained.add(order++, InitMilestone.JOB_LOADED);
        }

        @Initializer(after = InitMilestone.JOB_CONFIG_ADAPTED)
        public void jobConfigAdapted() {
            attained.add(order++, InitMilestone.JOB_CONFIG_ADAPTED);
        }

        public List<InitMilestone> getAttained() {
            return attained;
        }
    }

}
