/*
 * TransformExtensional.java
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

import org.jacop.constraints.Constraint;
import org.jacop.constraints.ExtensionalSupportVA;
import org.jacop.core.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * It defines an intialize listener which transforms part of the problem
 * into an extensional constraint by searching for all partial solutions
 * given the scope of the variables of interest.
 *
 * @author Radoslaw Szymanek and Krzysztof Kuchcinski
 * @version 4.9
 */

public class TransformExtensional implements InitializeListener {

    InitializeListener[] initializeChildListeners;

    /**
     * It contains all the information which will become variables in
     * the scope of the extensional constraint produced by this search
     * listener.
     */
    public List<IntVar> variablesTransformationScope = new ArrayList<IntVar>();

    /**
     * The limit of solutions upon reaching the transformation is abandoned and solution
     * progress normally without any transformation.
     */
    public int solutionLimit = 10000;

    static final boolean debug = false;

    public void executedAtInitialize(Store store) {

        // @todo methods to suggest the interesting scope of the transformation.
        // Search for all solutions given set of variables V
        // Set of variables V should be chosen in such a way that
        // a) the set of solutions is not huge
        // b) many constraints scope falls within set V
        // c) constraints are not well communicating/propagating on its own
        //    but are rather tight together.

        SelectChoicePoint<IntVar> select =
            new SimpleSelect<IntVar>(variablesTransformationScope.toArray(new IntVar[1]), new MostConstrainedStatic<IntVar>(),
                new IndomainMin<IntVar>());

        Search<IntVar> search = new DepthFirstSearch<IntVar>();

        search.getSolutionListener().searchAll(true);
        search.getSolutionListener().recordSolutions(true);
        search.getSolutionListener().setSolutionLimit(solutionLimit);
        search.setAssignSolution(false);

        boolean searchResult = search.labeling(store, select);

        // If solution limit has been reached then no transformation.
        searchResult &= !search.getSolutionListener().solutionLimitReached();

        // Create for all solutions an extensional constraints
        if (searchResult) {

            // All constraint which have scope within set V are removed

            for (Var v : variablesTransformationScope) {

                Constraint[][] varConstraints = v.dom().modelConstraints;
                int[] toEvaluate = v.dom().modelConstraintsToEvaluate;

                Set<Constraint> constraintsInQuestion = new HashSet<Constraint>();

                for (int i = 0; i < toEvaluate.length; i++)
                    for (int j = 0; j < toEvaluate[i]; j++)
                        constraintsInQuestion.add(varConstraints[i][j]);

                for (Constraint checkConstraint : constraintsInQuestion) {

                    boolean toBeRemoved = true;

                    for (Var m : checkConstraint.arguments())
                        if (!variablesTransformationScope.contains(m))
                            toBeRemoved = false;

                    if (toBeRemoved)
                        checkConstraint.removeConstraint();

                }

            }

            // Obtaining all solutions and creating an extensional constraint.

            int[][] solutions = new int[search.getSolutionListener().solutionsNo()][];
            for (int i = 1; i <= solutions.length; i++) {
                Domain[] currentSolution = search.getSolution(i);
                solutions[i - 1] = new int[currentSolution.length];
                for (int j = 0; j < currentSolution.length; j++)
                    solutions[i - 1][j] = ((IntDomain) currentSolution[j]).min();
            }

            IntVar[] vars = search.getSolutionListener().getVariables();

            ExtensionalSupportVA transformationIntoExtensionalConstraint = new ExtensionalSupportVA(vars, solutions);
            store.impose(transformationIntoExtensionalConstraint);

            if (debug)
                System.out.println(transformationIntoExtensionalConstraint);

        }

    }

    public void setChildrenListeners(InitializeListener[] children) {
        initializeChildListeners = children;
    }

    public void setChildrenListeners(InitializeListener child) {
        initializeChildListeners = new InitializeListener[1];
        initializeChildListeners[0] = child;
    }

}
