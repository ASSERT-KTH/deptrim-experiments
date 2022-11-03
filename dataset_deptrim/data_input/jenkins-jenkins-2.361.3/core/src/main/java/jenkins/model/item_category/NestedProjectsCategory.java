/*
 * The MIT License
 *
 * Copyright (c) 2016 CloudBees, Inc.
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

package jenkins.model.item_category;

import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * Designed for project hierarchies with folders.
 *
 * This category should be moved to cloudbees-folder-plugin short-term.
 * Really when upgrades its baseline to 2.0.
 *
 * @since 2.0
 */
@Restricted(DoNotUse.class)
@Extension(ordinal = -100)
public class NestedProjectsCategory extends ItemCategory {

    /**
     * TODO Make public when be moved to cloudbees-folder-plugin.
     */
    private static final String ID = "nested-projects";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return Messages.NestedProjects_Description();
    }

    @Override
    public String getDisplayName() {
        return Messages.NestedProjects_DisplayName();
    }

    @Override
    public int getMinToShow() {
        return ItemCategory.MIN_TOSHOW;
    }
}
