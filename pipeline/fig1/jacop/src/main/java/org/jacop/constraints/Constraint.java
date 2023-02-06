/*
 * Constraint.java
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

import static java.util.stream.Collectors.joining;

import org.jacop.api.*;
import org.jacop.core.Store;
import org.jacop.core.SwitchesPruningLogging;
import org.jacop.core.Var;
import java.util.*;
import java.util.stream.Stream;


/*
 * Standard unified interface/abstract class for all constraints.
 * <p>
 * Defines how to construct a constraint, impose, check satisfiability,
 * notSatisfiability, enforce consistency.
 *
 * @author Krzysztof Kuchcinski and Radoslaw Szymanek
 * @version 4.9
 */

public abstract class Constraint extends DecomposedConstraint<Constraint> {

    protected Constraint() {
    }

    protected Constraint(Var[]... vars) {
        setScope(vars);
    }

    protected Constraint(Stream<Var> vars) {
        setScope(vars);
    }

    protected Constraint(PrimitiveConstraint[] constraints) {
        setScope(constraints);
    }

    protected Constraint(Set<? extends Var> set) {
        setScope(set);
    }

    public boolean trace = SwitchesPruningLogging.traceConstraint;

    /**
     * It specifies the number id for a given constraint. All constraints
     * within the same type have unique number ids.
     */
    public int numberId;

    /**
     * It returns the variables in a scope of the constraint.
     *
     * @return variables in a scope of the constraint.
     */
    public Set<Var> arguments() {
        return scope;
    }

    /**
     * It specifies a set of variables that in the scope of this constraint.
     */
    protected Set<Var> scope;

    protected void setScope(Var... variables) {
        this.scope = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(variables)));
    }

    protected void setScope(Var[]... variables) {
        setScope(Arrays.stream(variables).map(Arrays::stream).flatMap(i -> i));
    }

    protected void setScope(Stream<Var> scope) {
        setScope(scope.toArray(Var[]::new));
    }

    protected void setScope(PrimitiveConstraint[] constraints) {
        setScope(Arrays.stream(constraints).map(Constraint::arguments).flatMap(Collection::stream));
    }

    protected void setScope(Set<? extends Var> set) {
        setScope(set.toArray(new Var[set.size()]));
    }

    public Set<PrimitiveConstraint> constraintScope;

    protected void setConstraintScope(PrimitiveConstraint... primitiveConstraints) {
        this.constraintScope = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(primitiveConstraints)));
    }

    /**
     * It is a (most probably incomplete) consistency function which removes the
     * values from variables domains. Only values which do not have any support
     * in a solution space are removed.
     *
     * @param store constraint store within which the constraint consistency is being checked.
     */
    public abstract void consistency(Store store);

    /**
     * It retrieves the pruning event which causes reevaluation of the
     * constraint.
     *
     * @param var variable for which pruning event is retrieved
     * @return it returns the int code of the pruning event (GROUND, BOUND, ANY, NONE)
     */
    public int getConsistencyPruningEvent(Var var) {

        // If consistency function mode
        if (consistencyPruningEvents != null) {
            Integer possibleEvent = consistencyPruningEvents.get(var);
            if (possibleEvent != null)
                return possibleEvent;
        }

        if (constraintScope != null && !constraintScope.isEmpty()) {

            int eventAcross =
                constraintScope.stream().filter(i -> i.arguments().contains(var)).mapToInt(i -> i.getNestedPruningEvent(var, true)).max()
                    .orElseGet(() -> Integer.MIN_VALUE);

            if (eventAcross != Integer.MIN_VALUE)
                return eventAcross;

        }

        return getDefaultConsistencyPruningEvent();

    }

    public abstract int getDefaultConsistencyPruningEvent();

    /**
     * It gives the id string of a constraint.
     *
     * @return string id of the constraint.
     */
    public String id() {
        String constraintType = this.getClass().getSimpleName();
        if (constraintType.equals(""))
            constraintType = this.getClass().getName() + "#";
        return constraintType + numberId;
    }

    /**
     * It imposes the constraint in a given store.
     *
     * @param store the constraint store to which the constraint is imposed to.
     */
    public void impose(Store store) {

        arguments().stream().forEach(i -> i.putModelConstraint(this, getConsistencyPruningEvent(i)));
        store.addChanged(this);
        store.countConstraint();
        if (constraintScope != null) {
            constraintScope.stream().forEach(i -> i.include(store));
        }
        if (this instanceof UsesQueueVariable)
            arguments().stream().forEach(i -> queueVariable(store.level, i));

        if (constraintScope != null) {
            Set<RemoveLevelLate> fixpoint = computeFixpoint(this, new HashSet<>());
            fixpoint.forEach(store::registerRemoveLevelLateListener);
        }

        if (this instanceof RemoveLevelLate)
            store.registerRemoveLevelLateListener((RemoveLevelLate)this);

        if (this instanceof Stateful) {
            Stateful c = (Stateful) this;
            if (c.isStateful()) {
                store.registerRemoveLevelListener(c);
            }
        }

    }

    private Set<RemoveLevelLate> computeFixpoint(Constraint c, Set<RemoveLevelLate> fixpoint) {
        if (c instanceof RemoveLevelLate)
            fixpoint.add((RemoveLevelLate)c);
        if (c.constraintScope != null)
            c.constraintScope.forEach(ic -> computeFixpoint(ic, fixpoint));
        return fixpoint;
    }

    /**
     * It imposes the constraint and adjusts the queue index.
     *
     * @param store      the constraint store to which the constraint is imposed to.
     * @param queueIndex the index of the queue in the store it is assigned to.
     */
    public void impose(Store store, int queueIndex) {

        assert (queueIndex < store.queueNo) : "Constraint queue number larger than permitted by store.";

        this.queueIndex = queueIndex;

        impose(store);

    }

    /**
     * This is a function called to indicate which variable in a scope of
     * constraint has changed. It also indicates a store level at which the
     * change has occurred.
     *
     * @param level the level of the store at which the change has occurred.
     * @param var   variable which has changed.
     */
    public void queueVariable(final int level, final Var var) {
    }

    /**
     * It removes the constraint by removing this constraint from all variables.
     */
    public void removeConstraint() {
        // Stream version is not used due to large performance overhead.
        for (Var v : arguments())
            if (!v.singleton())
                v.removeConstraint(this);
    }


    Var watchedVariableGrounded;

    public void setWatchedVariableGrounded(Var var) {
        watchedVariableGrounded = var;
    }

    public boolean watchedVariableGrounded() {
        if (watchedVariableGrounded == null || watchedVariableGrounded.singleton()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * It checks if the constraint has all variables in its scope grounded (singletons).
     *
     * @return true if all variables in constraint scope are singletons, false otherwise.
     */
    public boolean grounded() {

        if (!watchedVariableGrounded())
            return false;

        Optional<Var> stillNotGrounded = arguments().stream().filter(i -> !i.singleton()).findFirst();

        if (stillNotGrounded.isPresent()) {
            setWatchedVariableGrounded(stillNotGrounded.get());
            return false;
        } else {
            return true;
        }

    }

    /**
     * It checks if provided variables are grounded (singletons).
     *
     * @param vars variables to be checked if they are grounded.
     * @return true if all variables in constraint scope are singletons, false otherwise.
     */
    public boolean grounded(Var[] vars) {
        return !Arrays.stream(vars).filter(i -> !i.singleton()).findFirst().isPresent();
    }

    /**
     * It produces a string representation of a constraint state.
     */
    @Override public String toString() {
        return arguments().stream().map(i -> i.toString()).collect(joining(", ", id() + "(", ")"));
    }

    public static String intArrayToString(int[] array) {
        return Arrays.stream(array).mapToObj(i -> Integer.toString(i)).collect(joining(", ", "[", "]"));
    }

    /**
     * It specifies a constraint which if imposed by search will enhance
     * propagation of this constraint.
     *
     * @return Constraint enhancing propagation of this constraint.
     */
    public Constraint getGuideConstraint() {
        return null;
    }

    /**
     * This function provides a variable which assigned a value returned
     * by will enhance propagation of this constraint.
     *
     * @return Variable which is a base of enhancing constraint.
     */
    public Var getGuideVariable() {
        return null;
    }

    /**
     * This function provides a value which if assigned to a variable returned
     * by getGuideVariable() will enhance propagation of this constraint.
     *
     * @return Value which is a base of enhancing constraint.
     */
    public int getGuideValue() {
        return Integer.MAX_VALUE;
    }

    /**
     * This function allows to provide a guide feedback. If constraint does
     * not propose sufficiently good enhancing constraints it will be informed
     * so it has a chance to reexamine its efforts.
     *
     * @param feedback true if the guide was useful, false otherwise.
     */
    public void supplyGuideFeedback(boolean feedback) {
    }

    /**
     * It increases the weight of the variables in the constraint scope.
     */
    public void increaseWeight() {

        if (increaseWeight)
            arguments().forEach(v -> v.weight++);
    }

    /**
     * It allows to customize the event for a given variable which
     * causes the re-execution of the consistency method for a constraint.
     *
     * @param var          variable for which the events are customized.
     * @param pruningEvent the event which must occur to trigger execution of the consistency method.
     */
    public void setConsistencyPruningEvent(final Var var, final int pruningEvent) {

        if (consistencyPruningEvents == null)
            consistencyPruningEvents = new Hashtable<>();
        consistencyPruningEvents.put(var, pruningEvent);

    }


    /**
     * It returns the number of variables within a constraint scope.
     *
     * @return number of variables in the constraint scope.
     */
    public int numberArgs() {
        return scope.size();
    }

    /**
     * It specifies if the constraint allows domains of variables
     * in its scope only to shrink its domain with the progress
     * of search downwards.
     *
     * @return true, by default by all constraints.
     */
    public boolean requiresMonotonicity() {
        return true;
    }

    /**
     * It specifies if upon the failure of the constraint, all variables
     * in the constraint scope should have their weight increased.
     */
    public boolean increaseWeight = true;

    /**
     * It specifies the event which must occur in order for the consistency function to
     * be called.
     */
    public Hashtable<Var, Integer> consistencyPruningEvents;

    /**
     * It specifies if the constraint consistency function can be prematurely terminated
     * through other than FailureException exception.
     */
    public boolean earlyTerminationOK = false;

    /**
     * It specifies if the constraint consistency function requires consistency function
     * executed in one atomic step. A constraint can specify that if any other pruning
     * events are initiated by outside entity then the constraint may not work correctly
     * if the execution is continued, but it will work well if consistency() function is
     * restarted.
     */
    public boolean atomicExecution = true;

    /**
     * It imposes the decomposition of the given constraint in a given store.
     *
     * @param store the constraint store to which the constraint is imposed to.
     */
    @Override public void imposeDecomposition(Store store) {
        throw new UnsupportedOperationException();
    }

    /**
     * It returns an array list of constraint which are used to decompose this
     * constraint. It actually creates a decomposition (possibly also creating
     * variables), but it does not impose the constraint.
     *
     * @param store the constraint store in which context the decomposition takes place.
     * @return an array list of constraints used to decompose this constraint.
     */
    @Override public List<Constraint> decompose(final Store store) {
        throw new UnsupportedOperationException();
    }

    /*
     * Handling of AFC (accumulated failure count) for constraints
     *
     */
    double afcWeight = 1.0d;

    public double afc() {
        return afcWeight;
    }
    
    public void updateAFC(Set<Constraint> allConstraints, double decay) {
        afcWeight = (afcWeight + 1.0d) / decay;

        if (afcWeight > Double.MAX_VALUE * 1e-50) {
            // re-scale weights
            for (Constraint c : allConstraints)
                c.afcWeight *= 1e-150;
        }
    }
    
    /**
     * It is executed after the constraint has failed. It allows to clean some
     * data structures.
     */
    public void cleanAfterFailure() {
    }

    static int toInt(final float f) {
        if (f >= (float) Integer.MIN_VALUE && f <= (float) Integer.MAX_VALUE) {
            return (int) f;
        } else {
            throw new ArithmeticException("Overflow occurred " + f);
        }
    }

    static int toInt(final double f) {
        if (f >= (double) Integer.MIN_VALUE && f <= (double) Integer.MAX_VALUE) {
            return (int) f;
        } else {
            throw new ArithmeticException("Overflow occurred " + f);
        }
    }

    public static int long2int(long value) {
        if (value > (long) Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else if (value < (long) Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        else
            return (int) value;
    }
}
