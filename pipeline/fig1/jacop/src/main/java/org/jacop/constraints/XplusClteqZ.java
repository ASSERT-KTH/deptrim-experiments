/*
 * XplusClteqZ.java
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

package org.jacop.constraints;

import org.jacop.core.IntDomain;
import org.jacop.core.IntVar;
import org.jacop.core.Store;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Constraints X + C{@literal <=} Z.
 * <p>
 * Boundary consistency is used.
 *
 * @author Krzysztof Kuchcinski and Radoslaw Szymanek
 * @version 4.9
 */

public class XplusClteqZ extends PrimitiveConstraint {

    final static AtomicInteger idNumber = new AtomicInteger(0);

    /**
     * It specifies variable x in constraint x+c{@literal <=}z.
     */
    final public IntVar x;

    /**
     * It specifies constant c in constraint x+c{@literal <=} z.
     */
    final public int c;

    /**
     * It specifies variable z in constraint x+c{@literal <=} z.
     */
    final public IntVar z;

    /**
     * It constructs constraint X+C{@literal <=} Z.
     *
     * @param x variable x.
     * @param c constant c.
     * @param z variable z.
     */
    public XplusClteqZ(IntVar x, int c, IntVar z) {

        checkInputForNullness(new String[] {"x", "z"}, new Object[] {x, z});

        numberId = idNumber.incrementAndGet();

        this.x = x;
        this.c = c;
        this.z = z;

	checkForOverflow();
	
        setScope(x, z);
    }

    void checkForOverflow() {

        int sumMin = 0, sumMax = 0;

        sumMin = Math.addExact(sumMin, x.min());
        sumMax = Math.addExact(sumMax, x.max());

        sumMin = Math.addExact(sumMin, c);
        sumMax = Math.addExact(sumMax, c);

        Math.subtractExact(sumMin, z.max());
        Math.subtractExact(sumMax, z.min());
    }

    
    @Override public void consistency(final Store store) {

        x.domain.inMax(store.level, x, z.max() - c);
        z.domain.inMin(store.level, z, x.min() + c);

    }

    @Override protected int getDefaultNestedNotConsistencyPruningEvent() {
        return IntDomain.BOUND;
    }

    @Override protected int getDefaultNestedConsistencyPruningEvent() {
        return IntDomain.BOUND;
    }

    @Override protected int getDefaultNotConsistencyPruningEvent() {
        return IntDomain.BOUND;
    }

    @Override public int getDefaultConsistencyPruningEvent() {
        return IntDomain.BOUND;
    }

    @Override public void notConsistency(final Store store) {

        x.domain.inMin(store.level, x, z.min() - c + 1);
        z.domain.inMax(store.level, z, x.max() + c - 1);
    }

    @Override public boolean notSatisfied() {
        return x.min() + c > z.max();
    }

    @Override public boolean satisfied() {
        return x.max() + c <= z.min();
    }

    @Override public String toString() {

        return id() + " : XplusClteqZ(" + x + ", " + c + ", " + z + " )";
    }

}
