package jenkins.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.model.Node;
import java.io.IOException;
import jenkins.security.MasterToSlaveCallable;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JenkinsJVMRealTest {

    @ClassRule
    public static JenkinsRule j = new JenkinsRule();

    @Test
    public void isJenkinsJVM() throws Throwable {
        assertThat(new IsJenkinsJVM().call(), is(true));
        Node slave = j.createOnlineSlave();
        assertThat(slave.getChannel().call(new IsJenkinsJVM()), is(false));
    }

    public static class IsJenkinsJVM extends MasterToSlaveCallable<Boolean, IOException> {

        @Override
        public Boolean call() throws IOException {
            return JenkinsJVM.isJenkinsJVM();
        }
    }

}
