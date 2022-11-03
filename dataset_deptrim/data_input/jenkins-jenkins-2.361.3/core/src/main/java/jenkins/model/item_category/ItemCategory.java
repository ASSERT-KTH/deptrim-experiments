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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.RestrictedSince;
import hudson.model.TopLevelItemDescriptor;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * A category for {@link hudson.model.Item}s.
 *
 * @since 2.0
 */
public abstract class ItemCategory implements ExtensionPoint {

    /**
     * This field indicates how much non-default categories are required in
     * order to start showing them in Jenkins.
     * This field is restricted for the internal use only, because all other changes would cause binary compatibility issues.
     * See <a href="https://issues.jenkins.io/browse/JENKINS-36593">JENKINS-36593</a> for more info.
     */
    @Restricted(NoExternalUse.class)
    @RestrictedSince("2.14")
    public static final int MIN_TOSHOW = 1;

    /**
     * Helpful to set the order.
     */
    private int order = 1;

    /**
     * Identifier, e.g. "standaloneprojects", etc.
     *
     * @return the identifier
     */
    public abstract String getId();

    /**
     * The description in plain text
     *
     * @return the description
     */
    public abstract String getDescription();

    /**
     * A human readable name.
     *
     * @return the display name
     */
    public abstract String getDisplayName();

    /**
     * Minimum number of items required to show the category.
     *
     * @return the minimum items required
     */
    public abstract int getMinToShow();

    private void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    /**
     * A {@link ItemCategory} associated to this {@link TopLevelItemDescriptor}.
     *
     * @return A {@link ItemCategory}, if not found, {@link ItemCategory.UncategorizedCategory} is returned
     */
    @NonNull
    public static ItemCategory getCategory(TopLevelItemDescriptor descriptor) {
        int order = 0;
        ExtensionList<ItemCategory> categories = ExtensionList.lookup(ItemCategory.class);
        for (ItemCategory category : categories) {
            if (category.getId().equals(descriptor.getCategoryId())) {
                category.setOrder(++order);
                return category;
            }
            order++;
        }
        return new UncategorizedCategory();
    }

    /**
     * The default {@link ItemCategory}, if an item doesn't belong anywhere else, this is where it goes by default.
     */
    @Extension(ordinal = Integer.MIN_VALUE)
    public static final class UncategorizedCategory extends ItemCategory {

        public static final String ID = "uncategorized";

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getDescription() {
            return Messages.Uncategorized_Description();
        }

        @Override
        public String getDisplayName() {
            return Messages.Uncategorized_DisplayName();
        }

        @Override
        public int getMinToShow() {
            return ItemCategory.MIN_TOSHOW;
        }

    }

}
