package hudson.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Collections;
import java.util.TimeZone;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test cases for TimeZoneProperty
 */
public class TimeZonePropertyTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testEnsureTimeZoneIsNullByDefault() {
        String timeZone = TimeZoneProperty.forCurrentUser();
        assertNull(timeZone);
    }

    @Test
    public void testEnsureInvalidTimeZoneDefaultsToNull() throws IOException {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        User user = User.get("John Smith", true, Collections.emptyMap());
        SecurityContextHolder.getContext().setAuthentication(user.impersonate2());

        TimeZoneProperty tzp = new TimeZoneProperty("InvalidTimeZoneName");
        user.addProperty(tzp);

        assertNull(TimeZoneProperty.forCurrentUser());
    }

    @Test
    public void testSetUserDefinedTimeZone() throws IOException {
        String timeZone = TimeZone.getDefault().getID();
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        User user = User.get("John Smith", true, Collections.emptyMap());
        SecurityContextHolder.getContext().setAuthentication(user.impersonate2());

        assertNull(TimeZoneProperty.forCurrentUser());
        TimeZoneProperty tzp = new TimeZoneProperty(timeZone);
        user.addProperty(tzp);
        assertEquals(TimeZone.getDefault().getID(), TimeZoneProperty.forCurrentUser());
    }
}
