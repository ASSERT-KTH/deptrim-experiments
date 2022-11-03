/*
 * The MIT License
 *
 * Copyright (c) 2010, Kohsuke Kawaguchi
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

package hudson;

import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.ExtensionFilter;

/**
 * Discovered {@link Extension} object with a bit of metadata for Hudson.
 * This is a plain value object.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.356
 * @see ExtensionFinder
 * @see ExtensionFilter
 */
public class ExtensionComponent<T> implements Comparable<ExtensionComponent<T>> {
    private static final Logger LOG = Logger.getLogger(ExtensionComponent.class.getName());
    private final T instance;
    private final double ordinal;

    public ExtensionComponent(T instance, double ordinal) {
        this.instance = instance;
        this.ordinal = ordinal;
    }

    public ExtensionComponent(T instance, Extension annotation) {
        this(instance, annotation.ordinal());
    }

    public ExtensionComponent(T instance) {
        this(instance, 0);
    }

    /**
     * See {@link Extension#ordinal()}. Used to sort extensions.
     */
    public double ordinal() {
        return ordinal;
    }

    /**
     * The instance of the discovered extension.
     *
     * @return never null.
     */
    public T getInstance() {
        return instance;
    }

    /**
     * Checks if this component is a {@link Descriptor} describing the given type
     *
     * For example, {@code component.isDescriptorOf(Builder.class)}
     */
    public boolean isDescriptorOf(Class<? extends Describable> c) {
        return instance instanceof Descriptor && ((Descriptor) instance).isSubTypeOf(c);
    }

    /**
     * Sort {@link ExtensionComponent}s in the descending order of {@link #ordinal()}.
     */
    @Override
    public int compareTo(ExtensionComponent<T> that) {
        double a = this.ordinal();
        double b = that.ordinal();
        if (Double.compare(a, b) > 0) return -1;
        if (Double.compare(a, b) < 0) return 1;

        boolean thisIsDescriptor = false;
        String thisLabel = this.instance.getClass().getName();
        if (this.instance instanceof Descriptor) {
            try {
                thisLabel = Util.fixNull(((Descriptor) this.instance).getDisplayName());
                thisIsDescriptor = true;
            } catch (RuntimeException | LinkageError x) {
                LOG.log(Level.WARNING, "Failure during Descriptor#getDisplayName for " + this.instance.getClass().getName(), x);
            }
        }

        boolean thatIsDescriptor = false;
        String thatLabel = that.instance.getClass().getName();
        if (that.instance instanceof Descriptor) {
            try {
                thatLabel = Util.fixNull(((Descriptor) that.instance).getDisplayName());
                thatIsDescriptor = true;
            } catch (RuntimeException | LinkageError x) {
                LOG.log(Level.WARNING, "Failure during Descriptor#getDisplayName for " + that.instance.getClass().getName(), x);
            }
        }

        if (thisIsDescriptor) {
            if (thatIsDescriptor) {
                return thisLabel.compareTo(thatLabel);
            } else {
                return 1;
            }
        } else {
            if (thatIsDescriptor) {
                return -1;
            }
        }

        return thisLabel.compareTo(thatLabel);
    }
}
