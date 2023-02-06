/*
 * TaskReversedView.java
 * This file is part of JaCoP.
 * <p>
 * JaCoP is a Java Constraint Programming solver.
 * <p>
 * Copyright (C) 2000-2008 Krzysztof Kuchcinski and Radoslaw Szymanek
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * Notwithstanding any other provision of this License, the copyright
 * owners of this work supplement the terms of this License with terms
 * prohibiting misrepresentation of the origin of this work and requiring
 * that modified versions of this work be marked in reasonable ways as
 * different from the original version. This supplement of the license
 * terms is in accordance with Section 7 of GNU Affero General Public
 * License version 3.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jacop.constraints.cumulative;

import org.jacop.core.IntVar;

/*
 * Represents tasks for cumulative constraint
 *
 * @author Krzysztof Kuchcinski
 * @version 4.9
 */

class TaskReversedView extends TaskView {

    TaskReversedView(IntVar start, IntVar dur, IntVar res) {
        super(start, dur, res);
    }

    int lct() {
        return -start.min();
    }

    int ect() {
        return est() + dur.min();
    }

    int est() {
        return -start.max() - dur.max();
    }

    // last start time
    int lst() {
        return lct() - dur.min();
    }

    long env(long c) {
        return c * (long) est() + e();
    }

    void updateEdgeFind(int storeLevel, int lct) {
        int max = -lct - dur.min();
        if (max < start.max())
            start.domain.inMax(storeLevel, start, max);
    }

    void updateNotFirstNotLast(int storeLevel, int lct) {
        int min = -lct;
        if (min > start.min())
            start.domain.inMin(storeLevel, start, min);
    }

    void updateDetectable(int storeLevel, int lct) {
        int max = -lct - dur.min();
        if (max < start.max())
            start.domain.inMax(storeLevel, start, max);
    }

    boolean exists() {
        return dur.min() > 0 && res.min() > 0;
    }

    boolean maxNonZero() {
        return dur.max() > 0 && res.max() > 0;
    }

}
