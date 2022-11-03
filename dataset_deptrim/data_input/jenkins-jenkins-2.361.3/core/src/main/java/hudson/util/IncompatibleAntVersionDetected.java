/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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

import hudson.remoting.Which;
import java.io.IOException;
import java.net.URL;

/**
 * Model object used to display the error top page if
 * we find out that the container is picking up its own Ant and that's not 1.7.
 *
 * <p>
 * {@code index.jelly} would display a nice friendly error page.
 *
 * @author Kohsuke Kawaguchi
 */
public class IncompatibleAntVersionDetected extends BootFailure {
    private final Class antClass;

    public IncompatibleAntVersionDetected(Class antClass) {
        this.antClass = antClass;
    }

    @Override
    public String getMessage() {
        try {
            return "Incompatible Ant loaded from " + getWhereAntIsLoaded();
        } catch (IOException e) {
            return "Incompatible Ant loaded";
        }
    }

    public URL getWhereAntIsLoaded() throws IOException {
        return Which.classFileUrl(antClass);
    }
}
