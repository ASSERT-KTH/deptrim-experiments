/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
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

package hudson.cli;

import hudson.Extension;
import hudson.model.DirectlyModifiableView;
import hudson.model.TopLevelItem;
import hudson.model.View;
import java.util.List;
import org.kohsuke.args4j.Argument;

/**
 * @author ogondza
 * @since 1.570
 */
@Extension
public class RemoveJobFromViewCommand extends CLICommand {

    @Argument(usage = "Name of the view", required = true, index = 0)
    private View view;

    @Argument(usage = "Job names", required = true, index = 1)
    private List<TopLevelItem> jobs;

    @Override
    public String getShortDescription() {
        return Messages.RemoveJobFromViewCommand_ShortDescription();
    }

    @Override
    protected int run() throws Exception {
        view.checkPermission(View.CONFIGURE);

        if (!(view instanceof DirectlyModifiableView))
            throw new IllegalStateException("'" + view.getDisplayName() + "' view can not be modified directly");

        for (TopLevelItem job : jobs) {
            ((DirectlyModifiableView) view).remove(job);
        }

        return 0;
    }
}
