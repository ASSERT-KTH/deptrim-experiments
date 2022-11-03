/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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

package jenkins;

import hudson.remoting.Which;
import java.util.Map;
import jenkins.model.Jenkins;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jvnet.hudson.test.JellyTestSuiteBuilder;
import org.jvnet.hudson.test.PluginAutomaticTestBuilder;
import org.jvnet.hudson.test.PropertiesTestSuite;

/**
 * Runs checks on Jelly files and properties files in core.
 *
 * @author Kohsuke Kawaguchi
 * @see PluginAutomaticTestBuilder
 */
public class CoreAutomaticTestBuilder extends TestCase {

    /**
     * @see PluginAutomaticTestBuilder#build(Map)
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite();
        suite.addTest(JellyTestSuiteBuilder.build(Which.jarFile(Jenkins.class), true)); // also initializes the version
        suite.addTest(new PropertiesTestSuite(Which.jarFile(Jenkins.class)));
        return suite;
    }
}
