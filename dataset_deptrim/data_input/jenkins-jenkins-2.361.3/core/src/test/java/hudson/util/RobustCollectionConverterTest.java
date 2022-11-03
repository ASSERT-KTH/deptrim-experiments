/*
 * The MIT License
 *
 * Copyright (c) 2022 CloudBees, Inc.
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

package hudson.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.thoughtworks.xstream.security.InputManipulationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jenkins.util.xstream.CriticalXStreamException;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

public class RobustCollectionConverterTest {
    @Test
    public void workingByDefaultWithSimplePayload() {
        XStream2 xstream2 = new XStream2();

        Map<String, String> map = new HashMap<>();
        map.put("a", "gsdf");
        map.put("b", "");

        Set<Object> set = new HashSet<>();
        set.add(4L);
        set.add(5.67);
        set.add("tasd");
        set.add('z');
        set.add(Instant.now());

        // to get an ArrayList and not a Arrays.ArrayList
        List<Object> payload = new ArrayList<>(Arrays.asList(123, "abc", map, new Date(), set));

        String xmlContent = xstream2.toXML(payload);

        Object rawResult = xstream2.fromXML(xmlContent);
        assertEquals(payload, rawResult);
    }

    /**
     * As RobustCollectionConverter is the replacer of the default CollectionConverter
     * We had to patch it in order to not be impacted by CVE-2021-43859
     */
    // force timeout to prevent DoS due to test in the case the DoS prevention is broken
    @Test(timeout = 30 * 1000)
    @Issue("SECURITY-2602")
    public void dosIsPrevented_customProgrammaticallyTimeout() {
        XStream2 xstream2 = new XStream2();

        Set<Object> set = preparePayload();

        xstream2.setCollectionUpdateLimit(3);
        final String xml = xstream2.toXML(set);
        try {
            xstream2.fromXML(xml);
            fail("Thrown " + CriticalXStreamException.class.getName() + " expected");
        } catch (final CriticalXStreamException e) {
            Throwable cause = e.getCause();
            assertNotNull("A non-null cause of CriticalXStreamException is expected", cause);
            assertTrue("Cause of CriticalXStreamException is expected to be InputManipulationException", cause instanceof InputManipulationException);
            InputManipulationException ime = (InputManipulationException) cause;
            assertTrue("Limit expected in message", ime.getMessage().contains("exceeds 3 seconds"));
        }
    }

    @Test(timeout = 30 * 1000)
    @Issue("SECURITY-2602")
    public void dosIsPrevented_customPropertyTimeout() {
        String currentValue = System.getProperty(XStream2.COLLECTION_UPDATE_LIMIT_PROPERTY_NAME);
        try {
            System.setProperty(XStream2.COLLECTION_UPDATE_LIMIT_PROPERTY_NAME, "4");

            XStream2 xstream2 = new XStream2();

            Set<Object> set = preparePayload();

            final String xml = xstream2.toXML(set);
            try {
                xstream2.fromXML(xml);
                fail("Thrown " + CriticalXStreamException.class.getName() + " expected");
            } catch (final CriticalXStreamException e) {
                Throwable cause = e.getCause();
                assertNotNull("A non-null cause of CriticalXStreamException is expected", cause);
                assertTrue("Cause of CriticalXStreamException is expected to be InputManipulationException", cause instanceof InputManipulationException);
                InputManipulationException ime = (InputManipulationException) cause;
                assertTrue("Limit expected in message", ime.getMessage().contains("exceeds 4 seconds"));
            }
        } finally {
            if (currentValue == null) {
                System.clearProperty(XStream2.COLLECTION_UPDATE_LIMIT_PROPERTY_NAME);
            } else {
                System.setProperty(XStream2.COLLECTION_UPDATE_LIMIT_PROPERTY_NAME, currentValue);
            }
        }
    }

    // force timeout to prevent DoS due to test in the case the DoS prevention is broken
    @Test(timeout = 30 * 1000)
    @Issue("SECURITY-2602")
    public void dosIsPrevented_defaultTimeout() {
        XStream2 xstream2 = new XStream2();

        Set<Object> set = preparePayload();

        final String xml = xstream2.toXML(set);
        try {
            xstream2.fromXML(xml);
            fail("Thrown " + CriticalXStreamException.class.getName() + " expected");
        } catch (final CriticalXStreamException e) {
            Throwable cause = e.getCause();
            assertNotNull("A non-null cause of CriticalXStreamException is expected", cause);
            assertTrue("Cause of CriticalXStreamException is expected to be InputManipulationException", cause instanceof InputManipulationException);
            InputManipulationException ime = (InputManipulationException) cause;
            assertTrue("Limit expected in message", ime.getMessage().contains("exceeds 5 seconds"));
        }
    }

    // Inspired by https://github.com/x-stream/xstream/commit/e8e88621ba1c85ac3b8620337dd672e0c0c3a846#diff-9fde4ecf1bb4dc9850c031cb161960d2e61e069b386fa0b3db0d57e0e9f5baa
    // which seems to be inspired by https://owasp.org/www-community/vulnerabilities/Deserialization_of_untrusted_data
    private Set<Object> preparePayload() {
        /*
            On a i7-1185G7@3.00GHz (2021)
            Full test time:
            max=10 => ~1s
            max=15 => ~1s
            max=20 => ~1s
            max=25 => ~3s
            max=26 => ~6s
            max=27 => ~11s
            max=28 => ~22s
            max=29 => ~47s
            max=30 => >1m30
            max=32 => est. 6m

            With the protection in place, each test is taking ~15 seconds before the protection triggers
        */

        final Set<Object> set = new HashSet<>();
        Set<Object> s1 = set;
        Set<Object> s2 = new HashSet<>();
        for (int i = 0; i < 32; i++) {
            final Set<Object> t1 = new HashSet<>();
            final Set<Object> t2 = new HashSet<>();
            t1.add("a");
            t2.add("b");
            s1.add(t1);
            s1.add(t2);
            s2.add(t2);
            s2.add(t1);
            s1 = t1;
            s2 = t2;
        }
        return set;
    }
}
