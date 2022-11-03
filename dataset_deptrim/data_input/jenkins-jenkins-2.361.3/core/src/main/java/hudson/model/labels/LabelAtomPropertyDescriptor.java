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

package hudson.model.labels;

import hudson.Extension;
import hudson.model.Descriptor;

/**
 * {@link Descriptor} for {@link LabelAtom}.
 *
 * <p>
 * Put {@link Extension} on your descriptor implementation to have it auto-registered.
 *
 * <p>
 * When extending this class, override {@link Descriptor#getDisplayName()}. In the
 * context of LabelAtomPropertyDescriptor, this function is used to determine the label of
 * the checkbox shown in the label's configuration page to activate the extension.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.373
 */
public abstract class LabelAtomPropertyDescriptor extends Descriptor<LabelAtomProperty> {

}
