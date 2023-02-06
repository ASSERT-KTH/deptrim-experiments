/*
 * LargestDomainFloat.java
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

package org.jacop.floats.search;

import org.jacop.core.Var;
import org.jacop.floats.core.FloatVar;
import org.jacop.search.ComparatorVariable;

/**
 * Defines LargestDomainFloat comparator for Variables. Variable with the largest
 * domain has the priority.
 *
 * @param <T> it specifies the class of the variable being used in this variable selection method.
 * @author Krzysztof Kuchcinski and Radoslaw Szymanek
 * @version 4.9
 */

public class LargestDomainFloat<T extends Var> implements ComparatorVariable<T> {

    /**
     * It constructs variable comparator based on the largest domain priority.
     */
    public LargestDomainFloat() {
    }

    public int compare(double left, T var) {
        double right = ((FloatVar) var).getSizeFloat();

        if (left > right)
            return 1;
        if (left < right)
            return -1;
        return 0;
    }

    public int compare(T leftVar, T rightVar) {
        double left = ((FloatVar) leftVar).getSizeFloat();
        double right = ((FloatVar) rightVar).getSizeFloat();

        if (left > right)
            return 1;
        if (left < right)
            return -1;
        return 0;
    }

    public double metric(T var) {
        return ((FloatVar) var).getSizeFloat();
    }

}
