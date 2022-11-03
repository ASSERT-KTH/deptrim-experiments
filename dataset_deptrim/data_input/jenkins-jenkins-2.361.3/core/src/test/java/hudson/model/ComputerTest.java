package hudson.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import hudson.FilePath;
import hudson.security.ACL;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jenkins.model.Jenkins;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.springframework.security.core.Authentication;

/**
 * @author Kohsuke Kawaguchi
 */
public class ComputerTest {
    @Test
    public void testRelocate() throws Exception {
        File d = File.createTempFile("jenkins", "test");
        FilePath dir = new FilePath(d);
        try {
            dir.delete();
            dir.mkdirs();
            dir.child("slave-abc.log").touch(0);
            dir.child("slave-def.log.5").touch(0);

            Computer.relocateOldLogs(d);

            assertEquals(1, dir.list().size()); // asserting later that this one child is the logs/ directory
            assertTrue(dir.child("logs/slaves/abc/slave.log").exists());
            assertTrue(dir.child("logs/slaves/def/slave.log.5").exists());
        } finally {
            dir.deleteRecursive();
        }
    }

    @Issue("JENKINS-50296")
    @Test
    public void testThreadPoolForRemotingActsAsSystemUser() throws InterruptedException, ExecutionException {
        Future<Authentication> job = Computer.threadPoolForRemoting.submit(Jenkins::getAuthentication2);
        assertThat(job.get(), is(ACL.SYSTEM2));
    }
}
