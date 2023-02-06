/*
 * OrBoolVector.java
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

import org.jacop.core.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * If at least one variable from the list is equal 1 then result variable is equal 1 too.
 * Otherwise, result variable is equal to zero.
 * It restricts the domain of all x as well as result to be between 0 and 1.
 *
 * @author Krzysztof Kuchcinski and Radoslaw Szymanek
 * @version 4.9
 */

public class OrBoolVector extends PrimitiveConstraint {

    static AtomicInteger idNumber = new AtomicInteger(0);

    /**
     * It specifies a list of variables among which one must be equal to 1 to set result variable to 1.
     */
    public IntVar[] list;

    /**
     * It specifies variable result, storing the result of or function performed a list of variables.
     */
    public IntVar result;

    /**
     * It specifies the length of the list of variables.
     */
    final int l;

    /*
     * Defines first position of the variable that is not ground to 0
     */
    private TimeStamp<Integer> position;

    /**
     * It constructs orBool.
     *
     * @param list   list of x's which one of them must be equal 1 to make result equal 1.
     * @param result variable which is equal 0 if none of x is equal to zero.
     */
    public OrBoolVector(IntVar[] list, IntVar result) {

        checkInputForNullness("list", list);
        checkInputForNullness("result", new Object[] {result});

        this.numberId = idNumber.incrementAndGet();

        Set<IntVar> varSet = new HashSet<IntVar>();
        Arrays.stream(list).forEach(varSet::add);
        this.l = varSet.size();
        this.list = varSet.toArray(new IntVar[varSet.size()]);
        this.result = result;

        assert (checkInvariants() == null) : checkInvariants();

        if (l > 2)
            queueIndex = 1;
        else
            queueIndex = 0;

        setScope(Stream.concat(Arrays.stream(list), Stream.of(result)));
    }

    /**
     * It constructs orBool.
     *
     * @param list   list of x's which one of them must be equal 1 to make result equal 1.
     * @param result variable which is equal 0 if none of x is equal to zero.
     */
    public OrBoolVector(List<? extends IntVar> list, IntVar result) {
        this(list.toArray(new IntVar[list.size()]), result);
    }

    /**
     * It checks invariants required by the constraint. Namely that
     * boolean variables have boolean domain.
     *
     * @return the string describing the violation of the invariant, null otherwise.
     */
    public String checkInvariants() {

        for (IntVar var : list)
            if (var.min() < 0 || var.max() > 1)
                return "Variable " + var + " does not have boolean domain";

        return null;
    }

    @Override protected int getDefaultNestedNotConsistencyPruningEvent() {
        return IntDomain.GROUND;
    }

    @Override protected int getDefaultNestedConsistencyPruningEvent() {
        return IntDomain.ANY;
    }

    @Override public int getDefaultConsistencyPruningEvent() {
        return IntDomain.BOUND;
    }

    @Override protected int getDefaultNotConsistencyPruningEvent() {
        return IntDomain.GROUND;
    }

    @Override public void include(Store store) {
        position = new TimeStamp<Integer>(store, 0);
    }

    public void consistency(Store store) {

        int start = position.value();
        int index_01 = l - 1;

        for (int i = start; i < l; i++) {
            if (list[i].min() == 1) {
                result.domain.inValue(store.level, result, 1);
                removeConstraint();
                return;
            } else if (list[i].max() == 0) {
                swap(start, i);
                start++;
            }
        }
        position.update(start);

        if (start == l)
            result.domain.inValue(store.level, result, 0);

        // for case >, then the in() will fail as the constraint should.
        if (result.min() == 1 && start >= l - 1)
            list[index_01].domain.inValue(store.level, list[index_01], 1);

        if (result.max() == 0 && start < l)
            for (int i = start; i < l; i++)
                list[i].domain.inValue(store.level, list[i], 0);

        if ((l - start) < 3)
            queueIndex = 0;

    }

    private void swap(int i, int j) {
        if (i != j) {
            IntVar tmp = list[i];
            list[i] = list[j];
            list[j] = tmp;
        }
    }

    @Override public void notConsistency(Store store) {

        // do {

        //     store.propagationHasOccurred = false;

        int start = position.value();
        int index_01 = l - 1;

        for (int i = start; i < l; i++) {
            if (list[i].min() == 1) {
                result.domain.inValue(store.level, result, 0);
                return;
            } else if (list[i].max() == 0) {
                swap(start, i);
                start++;
            }
        }
        position.update(start);

        if (start == l)
            result.domain.inValue(store.level, result, 1);

        // for case >, then the in() will fail as the constraint should.
        if (result.min() == 1 && start < l)
            for (int i = 0; i < l; i++)
                list[i].domain.inValue(store.level, list[i], 0);

        if (result.max() == 0 && start >= l - 1)
            list[index_01].domain.inValue(store.level, list[index_01], 1);

        // } while (store.propagationHasOccurred);

        if ((l - start) < 3)
            queueIndex = 0;
    }

    @Override public boolean satisfied() {

        int start = position.value();

        if (result.max() == 0) {

            for (int i = start; i < l; i++)
                if (list[i].max() != 0)
                    return false;
                else {
                    swap(start, i);
                    start++;
                }
            position.update(start);

            return true;

        } else {

            if (result.min() == 1) {

                for (int i = start; i < l; i++)
                    if (list[i].min() == 1)
                        return true;
                    else if (list[i].max() == 0) {
                        swap(start, i);
                        start++;
                    }
            }
            position.update(start);
        }

        return false;

    }

    @Override public boolean notSatisfied() {

        int start = position.value();

        int x1 = 0;
        int x0 = start;

        for (int i = start; i < l; i++) {
            if (list[i].min() == 1)
                x1++;
            else if (list[i].max() == 0) {
                x0++;
                swap(start, i);
                start++;
            }
        }
        position.update(start);

        return (x0 == l && result.min() == 1) || (x1 != 0 && result.max() == 0);

    }

    @Override public String toString() {

        StringBuffer resultString = new StringBuffer(id());

        resultString.append(" : orBool([ ");
        for (int i = 0; i < l; i++) {
            resultString.append(list[i]);
            if (i < l - 1)
                resultString.append(", ");
        }
        resultString.append("], ");
        resultString.append(result);
        resultString.append(")");
        return resultString.toString();
    }

    List<Constraint> constraints;

    @Override public List<Constraint> decompose(Store store) {

        constraints = new ArrayList<Constraint>();

        PrimitiveConstraint[] orConstraints = new PrimitiveConstraint[l];

        IntervalDomain booleanDom = new IntervalDomain(0, 1);

        for (int i = 0; i < orConstraints.length; i++) {
            orConstraints[0] = new XeqC(list[i], 1);
            constraints.add(new In(list[i], booleanDom));
        }

        constraints.add(new In(result, booleanDom));

        constraints.add(new Eq(new Or(orConstraints), new XeqC(result, 1)));

        return constraints;
    }

    @Override public void imposeDecomposition(Store store) {

        if (constraints == null)
            constraints = decompose(store);

        for (Constraint c : constraints)
            store.impose(c, queueIndex);

    }

}
