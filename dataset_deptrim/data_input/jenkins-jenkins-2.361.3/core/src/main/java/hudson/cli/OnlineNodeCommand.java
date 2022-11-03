/*
 * The MIT License
 *
 * Copyright 2015 Red Hat, Inc.
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

import hudson.AbortException;
import hudson.Extension;
import hudson.model.Computer;
import java.util.HashSet;
import java.util.List;
import org.kohsuke.args4j.Argument;

/**
 * CLI Command, which moves the node to the online state.
 * @author pjanouse
 * @since 1.642
 */
@Extension
public class OnlineNodeCommand extends CLICommand {

    @Argument(metaVar = "NAME", usage = "Agent name, or empty string for built-in node", required = true, multiValued = true)
    private List<String> nodes;

    @Override
    public String getShortDescription() {
        return Messages.OnlineNodeCommand_ShortDescription();
    }

    @Override
    protected int run() throws Exception {
        boolean errorOccurred = false;
        final HashSet<String> hs = new HashSet<>(nodes);

        for (String node_s : hs) {
            try {
                Computer computer = Computer.resolveForCLI(node_s);
                computer.cliOnline();
            } catch (Exception e) {
                if (hs.size() == 1) {
                    throw e;
                }

                final String errorMsg = node_s + ": " + e.getMessage();
                stderr.println(errorMsg);
                errorOccurred = true;
                continue;
            }
        }

        if (errorOccurred) {
            throw new AbortException(CLI_LISTPARAM_SUMMARY_ERROR_TEXT);
        }
        return 0;
    }
}
