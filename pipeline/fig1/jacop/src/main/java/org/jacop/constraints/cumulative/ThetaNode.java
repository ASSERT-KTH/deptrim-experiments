/*
 * ThetaNode.java
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

/**
 * Implements ThetaNode for ThetaTree.
 *
 * @author Krzysztof Kuchcinski
 * @version 4.9
 */

class ThetaNode extends TreeNode {

    // Theta parameters
    int ect;  // erliest complition time
    int p;    // processing time /duration)

    // values of ect and p that are stored here all the time, even when node is not enabled
    int ectT;
    int pT;

    ThetaNode() {
    }

    void assignValues() {
        ect = ectT;
        p = pT;
    }

    public String toString() {
        String taskNo = task != null ? "\ntask = " + task.index : "";
        return "node: " + index + taskNo + "\nect = " + ect + "\np = " + p;
    }
}
