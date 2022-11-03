/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

package hudson.lifecycle;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

/**
 * {@link Lifecycle} for Hudson installed as SMF service.
 *
 * @author Kohsuke Kawaguchi
 */
public class SolarisSMFLifecycle extends Lifecycle {
    /**
     * In SMF managed environment, just commit a suicide and the service will be restarted by SMF.
     */
    @Override
    @SuppressFBWarnings(value = "DM_EXIT", justification = "Exit is really intended.")
    public void restart() throws IOException, InterruptedException {
        Jenkins jenkins = Jenkins.getInstanceOrNull(); // guard against repeated concurrent calls to restart
        try {
            if (jenkins != null) {
                jenkins.cleanUp();
            }
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, "Failed to clean up. Restart will continue.", e);
        }
        System.exit(0);
    }

    private static final Logger LOGGER = Logger.getLogger(SolarisSMFLifecycle.class.getName());
}
