/*
 * FSMTransition.java
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

package org.jacop.util.fsm;

import org.jacop.core.IntDomain;

import java.util.Set;

/**
 * @author Radoslaw Szymanek and Krzysztof Kuchcinski
 * @version 4.9
 */

public class FSMTransition {

    /**
     * It specifies the domain associated with the transition.
     */
    public IntDomain domain;

    /**
     * It specifies the successor state we arrive to after taking the transition.
     */
    public FSMState successor;

    /**
     * It constructs a finite machine state transition.
     *
     * @param domain the domain which triggers the transition.
     * @param state  the successor state reached by a transition.
     */
    public FSMTransition(IntDomain domain, FSMState state) {
        this.domain = domain;
        this.successor = state;
    }


    /**
     * It performs a clone of a transition with copying the attributes too.
     *
     * @param states a list of states which have been already copied.
     * @return the transition clone.
     */
    public FSMTransition deepClone(Set<FSMState> states) {

        return new FSMTransition(domain, successor.deepClone(states));

    }

    @Override public int hashCode() {
        return successor.id;
    }

    @Override public boolean equals(Object o) {

        if (o == null)
            return false;

        if (o == this)
            return true;

        FSMTransition compareTo = (FSMTransition) o;

        if (compareTo.successor.equals(successor) && compareTo.domain.eq(domain))
            return true;

        return false;

    }

    @Override public String toString() {
        return successor.toString() + "@" + domain.toString();
    }

}
