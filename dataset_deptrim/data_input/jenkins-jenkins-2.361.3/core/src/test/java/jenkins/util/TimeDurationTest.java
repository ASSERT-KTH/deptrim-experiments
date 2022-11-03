package jenkins.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.jvnet.hudson.test.Issue;

@Issue("JENKINS-44052")
public class TimeDurationTest {

    @Test
    public void fromString() {
        assertEquals(1, TimeDuration.fromString("1").getTimeInMillis());

        assertEquals(1000, TimeDuration.fromString("1sec").getTimeInMillis());
        assertEquals(1000, TimeDuration.fromString("1secs").getTimeInMillis());
        assertEquals(1000, TimeDuration.fromString("1 secs ").getTimeInMillis());
        assertEquals(1000, TimeDuration.fromString(" 1 secs ").getTimeInMillis());
        assertEquals(1, TimeDuration.fromString(" 1 secs ").getTimeInSeconds());

        assertEquals(21000, TimeDuration.fromString(" 21  secs ").getTimeInMillis());
        assertEquals(21, TimeDuration.fromString(" 21  secs ").getTimeInSeconds());
    }

}
