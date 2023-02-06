/*
 * IndomainSetMax.java
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

package org.jacop.set.search;

import org.jacop.core.IntDomain;
import org.jacop.core.Store;
import org.jacop.search.Indomain;
import org.jacop.set.core.SetVar;
import java.util.Random;

/**
 * IndomainMin - implements enumeration method based on the selection of the
 * maximal value in the domain of variable.
 *
 * @param <T> type of variable being used in search.
 * @author Krzysztof Kuchcinski and Radoslaw Szymanek
 * @version 4.9
 */

public class IndomainSetRandom<T extends SetVar> implements Indomain<T> {

    /**
     * It creates random indomain heuristic with randomly generated seed for
     * random generated.
     */
    public IndomainSetRandom() {
        seed = (Store.seedPresent()) ? new Random(Store.getSeed()) : new Random();
    }

    /**
     * It creates random indomain heuristic according to the provided random
     * generator.
     *
     * @param seed seed for random number genrator
     */
    public IndomainSetRandom(Random seed) {
        this.seed = seed;
    }

    Random seed;

    public int indomain(T var) {

        IntDomain difference = var.domain.lub().subtract(var.domain.glb());

        int no = seed.nextInt(difference.getSize());

        return difference.getElementAt(no);

    }

}
