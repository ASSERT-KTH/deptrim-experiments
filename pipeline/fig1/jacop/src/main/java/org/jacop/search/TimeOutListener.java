/*
 * TimeOutListener.java
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

package org.jacop.search;


/**
 * It defines an interface of the class which wants to be informed about timeout
 * occurring during search, as well as the number of solutions found before
 * timeout has occurred.
 *
 * @author Radoslaw Szymanek and Krzysztof Kuchcinski
 * @version 4.9
 */

public interface TimeOutListener {

    /**
     * It is executed right after time out is determined.
     *
     * @param solutionsNo number of solutions found before the timeout occurred.
     */

    void executedAtTimeOut(int solutionsNo);

    /**
     * It sets children listeners for this timeout listener.
     *
     * @param children list of children listeners.
     */
    void setChildrenListeners(TimeOutListener[] children);

    /**
     * It sets child listener for this timeout listener.
     *
     * @param child child listener for this timeout listener.
     */
    void setChildrenListeners(TimeOutListener child);

}
