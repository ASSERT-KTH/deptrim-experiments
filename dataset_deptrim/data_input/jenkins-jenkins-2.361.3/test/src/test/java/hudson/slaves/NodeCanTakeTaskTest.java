/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
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

package hudson.slaves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.Result;
import hudson.model.Slave;
import hudson.model.queue.CauseOfBlockage;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

public class NodeCanTakeTaskTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Issue({"JENKINS-6598", "JENKINS-38514"})
    @Test
    public void takeBlockedByProperty() throws Exception {
        // Set built-in node executor count to zero to force all jobs to agents
        r.jenkins.setNumExecutors(0);
        Slave slave = r.createSlave();
        FreeStyleProject project = r.createFreeStyleProject();

        // First, attempt to run our project before adding the property
        r.buildAndAssertSuccess(project);

        // Add the build-blocker property and try again
        slave.getNodeProperties().add(new RejectAllTasksProperty());

        assertThrows(TimeoutException.class, () -> project.scheduleBuild2(0).get(10, TimeUnit.SECONDS));
        List<BuildableItem> buildables = r.jenkins.getQueue().getBuildableItems();
        assertNotNull(buildables);
        assertEquals(1, buildables.size());

        BuildableItem item = buildables.get(0);
        assertEquals(project, item.task);
        assertNotNull(item.getCauseOfBlockage());
        assertEquals("rejecting everything", item.getCauseOfBlockage().getShortDescription());
    }

    private static class RejectAllTasksProperty extends NodeProperty<Node> {
        @Override
        public CauseOfBlockage canTake(BuildableItem item) {
            return new CauseOfBlockage() {
                @Override
                public String getShortDescription() {
                    return "rejecting everything";
                }
            };
        }
    }

    @Test
    public void becauseNodeIsBusy() throws Exception {
        Slave slave = r.createSlave();
        FreeStyleProject project = r.createFreeStyleProject();
        project.setAssignedNode(slave);
        project.setConcurrentBuild(true);
        project.getBuildersList().add(new SleepBuilder(Long.MAX_VALUE));
        FreeStyleBuild build = project.scheduleBuild2(0).waitForStart(); // consume the one executor
        project.scheduleBuild2(0); // now try to reschedule
        Queue.Item item;
        while ((item = r.jenkins.getQueue().getItem(project)) == null || !item.isBuildable()) {
            Thread.sleep(100);
        }
        assertEquals(hudson.model.Messages.Queue_WaitingForNextAvailableExecutorOn(slave.getDisplayName()), item.getWhy());
        build.doStop();
        r.assertBuildStatus(Result.ABORTED, r.waitForCompletion(build));
    }

}
