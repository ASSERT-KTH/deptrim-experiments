/*
 * VariablesParameters.java
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

package org.jacop.fz;

import org.jacop.constraints.XeqY;
import org.jacop.constraints.Constraint;
import org.jacop.core.*;
import org.jacop.floats.constraints.PeqQ;
import org.jacop.floats.core.FloatVar;
import org.jacop.set.constraints.AeqB;
import org.jacop.set.constraints.AeqS;
import org.jacop.set.core.BoundSetDomain;
import org.jacop.set.core.SetDomain;
import org.jacop.set.core.SetVar;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * TODO, a short description what it does and how it is used. Remark,
 * it would be beneficial if all the methods were described, like
 * generateParameters(...) below.
 *
 * @author Krzysztof Kuchcinski
 * @version 4.9
 */
public class VariablesParameters implements ParserTreeConstants {

    static final boolean interval = false; // selection of interval or dense, if possible, domain
                                           // for variables

    static final boolean checkBounds = false; // whether to check bounds of IntVar before creation;
                                              // if they are not in interval
                                              // IntDomain.MinInt..IntDomainMaxInt raise Arithmetic
                                              // exception

    public static final double MIN_FLOAT = -1e150;
    public static final double MAX_FLOAT = 1e150;
    public static final int MIN_INT = IntDomain.MinInt;
    public static final int MAX_INT = IntDomain.MaxInt;

    Tables dictionary;
    int lowInterval;
    int highInterval;
    double lowFloatInterval;
    double highFloatInterval;

    ArrayList<Integer> intList;
    IntervalDomain rangeDomain;
    HashSet<String> annotations;
    ArrayList<IntDomain> indexBounds;

    int numberBooleanVariables = 0;
    int numberFloatVariables = 0;
    int numberSetVariables = 0;

    Options options;
    boolean debug = false;
    
    /**
     * It constructs variables parameters.
     */
    public VariablesParameters() {
    }

    void setOptions(Options options) {
        this.options = options;
        debug = options.debug();
    }
    
    /**
     * It generates a parameter from a given node and stores information about it in the table.
     *
     * @param node  the node from which the parameter is being generated.
     * @param table the table where the parameters are being stored.
     */
    void generateParameters(SimpleNode node, Tables table) {

        dictionary = table;
        annotations = new HashSet<String>();

        int type = getType(node);

        int initChild = getAnnotations(node, 1);

        // node.dump("");
        // System.out.println("*** Type = " + type + " init index = " + initChild);
        // System.out.println("*** Annotations: " + annotations);

        String ident;
        int val;
        IntDomain setValue;
        switch (type) {
            case 0: // int
            case 1: // int interval
            case 2: // int list
            case 3: // bool
                ident = ((ASTVarDeclItem) node).getIdent();
                val = getScalarFlatExpr(node, initChild);
                table.addInt(ident, val);
                break;
            case 4: // set int
            case 5: // set interval
            case 6: // set list
            case 7: // bool set
            case 10: // range set
                ident = ((ASTVarDeclItem) node).getIdent();
                setValue = getSetLiteral(node, initChild);
                table.addSet(ident, setValue);
                break;
            case 8: // float
                ident = ((ASTVarDeclItem) node).getIdent();
                double valFloat = getScalarFlatExprFloat(node, initChild);
                table.addFloat(ident, valFloat);
                break;
            default:
                throw new IllegalArgumentException("Not supported type in parameter; compilation aborted.");
        }
    }

    void generateVariables(SimpleNode node, Tables table, Store store) {

        dictionary = table;
        annotations = new HashSet<String>();
        boolean var_introduced = false;
        boolean output_var = false;
        boolean is_defined_var = false;

        // node.dump("*   ");

        int type = getType(node);

        int initChild = getAnnotations(node, 1);
        // System.out.println("*** Type = " + type + " init index = " + initChild);
        // System.out.println("*** Annotations: " + annotations);

        if (annotations.contains("var_is_introduced"))
            var_introduced = true;
        if (annotations.contains("output_var"))
            output_var = true;
        if (annotations.contains("is_defined_var"))
            is_defined_var = true;

        String ident;
        IntVar varInt;
        SetVar varSet;
        FloatVar varFloat;
        BooleanVar boolVar;
        IntDomain setValue;
        int initVal;
        double initValFloat;
        IntVar initVar;
        FloatVar initVarFloat;
        switch (type) {
            case 0: // int
                ident = ((ASTVarDeclItem) node).getIdent();
                varInt = new IntVar(store, ident, MIN_INT, MAX_INT);

                table.addVariable(ident, varInt);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_int(node, initChild)) {
                        initVal = getScalarFlatExpr(node, initChild);
                        varInt.domain.inValue(store.level, varInt, initVal);
                    } else {
                        initVar = getScalarFlatExpr_var(store, node, initChild);
                        XeqY c = new XeqY(varInt, initVar);
                        pose(store, c);
                    }

                }

                //              if (!is_defined_var || output_var)
                table.addSearchVar(varInt);
                if (output_var)
                    table.addOutVar(varInt);
                break;
            case 1: // int interval
                ident = ((ASTVarDeclItem) node).getIdent();

                if (lowInterval > highInterval)
                    throw Store.failException;

                if (checkBounds)
                    if (lowInterval < IntDomain.MinInt || highInterval > IntDomain.MaxInt)
                        throw new ArithmeticException("Bounds for " + ident + ": " + lowInterval + ".." + highInterval + " are too low/high");

                if (interval)
                    varInt = new IntVar(store, ident, new IntervalDomain(lowInterval, highInterval));
                else
                    varInt = new IntVar(store, ident, lowInterval, highInterval);

                table.addVariable(ident, varInt);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_int(node, initChild)) {
                        initVal = getScalarFlatExpr(node, initChild);
                        varInt.domain.inValue(store.level, varInt, initVal);
                    } else {
                        initVar = getScalarFlatExpr_var(store, node, initChild);
                        XeqY c = new XeqY(varInt, initVar);
                        pose(store, c);
                    }

                }
                //              if (!is_defined_var || output_var)
                table.addSearchVar(varInt);
                if (output_var)
                    table.addOutVar(varInt);
                break;
            case 2: // int list
                ident = ((ASTVarDeclItem) node).getIdent();
                varInt = new IntVar(store, ident);
                for (Integer e : intList) {
                    int element = e.intValue();

                    if (checkBounds)
                        if (element < IntDomain.MinInt || element > IntDomain.MaxInt)
                            throw new ArithmeticException("Domain value for " + ident + " is too high/low (" + element + ")");

                    ((IntVar) varInt).addDom(element, element);
                }
                table.addVariable(ident, varInt);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_int(node, initChild)) {
                        initVal = getScalarFlatExpr(node, initChild);
                        varInt.domain.inValue(store.level, varInt, initVal);
                    } else {
                        initVar = getScalarFlatExpr_var(store, node, initChild);
                        XeqY c = new XeqY(varInt, initVar);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchVar(varInt);
                if (output_var)
                    table.addOutVar(varInt);
                break;
            case 3: // bool
                ident = ((ASTVarDeclItem) node).getIdent();
                boolVar = new BooleanVar(store, ident);
                table.addVariable(ident, boolVar);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_int(node, initChild)) {
                        initVal = getScalarFlatExpr(node, initChild);
                        boolVar.domain.inValue(store.level, boolVar, initVal);
                    } else {
                        initVar = getScalarFlatExpr_var(store, node, initChild);
                        XeqY c = new XeqY(boolVar, initVar);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchVar(boolVar);
                if (output_var)
                    table.addOutVar(boolVar);
                numberBooleanVariables++;
                break;
            case 4: // set int
                ident = ((ASTVarDeclItem) node).getIdent();
                varSet = new SetVar(store, ident, new BoundSetDomain(MIN_INT, MAX_INT));
                table.addSetVariable(ident, (SetVar) varSet);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_set(node, initChild)) {
                        setValue = getSetLiteral(node, initChild);
                        AeqS c = new AeqS((SetVar) varSet, setValue);
                        pose(store, c);
                    } else {
                        Var initSetVar = getSetFlatExpr_var(store, node, initChild);
                        AeqB c = new AeqB((SetVar) varSet, (SetVar) initSetVar);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchSetVar(varSet);
                if (output_var)
                    table.addOutVar(varSet);
                numberSetVariables++;
                break;
            case 5: // set interval
                ident = ((ASTVarDeclItem) node).getIdent();

                if (lowInterval > highInterval)
                    varSet = new SetVar(store, ident, new BoundSetDomain());
                else
                    varSet =
                        new SetVar(store, ident, new BoundSetDomain(new IntervalDomain(), new IntervalDomain(lowInterval, highInterval)));

                table.addSetVariable(ident, (SetVar) varSet);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_set(node, initChild)) {
                        setValue = getSetLiteral(node, initChild);
                        AeqS c = new AeqS((SetVar) varSet, setValue);
                        pose(store, c);
                    } else {
                        Var initSetVar = getSetFlatExpr_var(store, node, initChild);
                        AeqB c = new AeqB((SetVar) varSet, (SetVar) initSetVar);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchSetVar(varSet);
                if (output_var)
                    table.addOutVar(varSet);
                numberSetVariables++;
                break;
            case 6: // set list
                ident = ((ASTVarDeclItem) node).getIdent();
                SetDomain dom = new BoundSetDomain();
                for (Integer e : intList)
                    dom.addDom(e.intValue(), e.intValue());
                varSet = new SetVar(store, ident, dom);
                table.addSetVariable(ident, (SetVar) varSet);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_set(node, initChild)) {
                        setValue = getSetLiteral(node, initChild);
                        AeqS c = new AeqS((SetVar) varSet, setValue);
                        pose(store, c);
                    } else {
                        Var initSetVar = getSetFlatExpr_var(store, node, initChild);
                        AeqB c = new AeqB((SetVar) varSet, (SetVar) initSetVar);
                        pose(store, c);
                    }

                }

                //              if (!is_defined_var || output_var)
                table.addSearchSetVar(varSet);
                if (output_var)
                    table.addOutVar(varSet);
                numberSetVariables++;
                break;
            case 10:
                ident = ((ASTVarDeclItem) node).getIdent();
                IntDomain domR = rangeDomain;

                varSet = new SetVar(store, ident, new BoundSetDomain(new IntervalDomain(), rangeDomain));
                table.addSetVariable(ident, (SetVar) varSet);

                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_set(node, initChild)) {
                        setValue = getSetLiteral(node, initChild);
                        AeqS c = new AeqS((SetVar) varSet, setValue);
                        pose(store, c);
                    } else {
                        Var initSetVar = getSetFlatExpr_var(store, node, initChild);
                        AeqB c = new AeqB((SetVar) varSet, (SetVar) initSetVar);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchSetVar(varSet);
                if (output_var)
                    table.addOutVar(varSet);
                numberSetVariables++;

                break;
            case 7: // bool set
                ident = ((ASTVarDeclItem) node).getIdent();
                varSet = new SetVar(store, ident, new BoundSetDomain(0, 1));
                table.addSetVariable(ident, (SetVar) varSet);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_set(node, initChild)) {
                        setValue = getSetLiteral(node, initChild);
                        AeqS c = new AeqS((SetVar) varSet, setValue);
                        pose(store, c);
                    } else {
                        Var initSetVar = getSetFlatExpr_var(store, node, initChild);
                        AeqB c = new AeqB((SetVar) varSet, (SetVar) initSetVar);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchSetVar(varSet);
                if (output_var)
                    table.addOutVar(varSet);
                numberSetVariables++;
                break;
            case 8: // float
                ident = ((ASTVarDeclItem) node).getIdent();
                varFloat = new FloatVar(store, ident, MIN_FLOAT, MAX_FLOAT);

                table.addFloatVariable(ident, varFloat);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_float(node, initChild)) {
                        initValFloat = getScalarFlatExprFloat(node, initChild);
                        varFloat.domain.in(store.level, varFloat, initValFloat, initValFloat);
                    } else {
                        initVarFloat = getScalarFlatExpr_varFloat(store, node, initChild);
                        PeqQ c = new PeqQ(varFloat, initVarFloat);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchFloatVar(varFloat);
                if (output_var)
                    table.addOutVar(varFloat);
                numberFloatVariables++;
                break;
            case 9: // float interval
                ident = ((ASTVarDeclItem) node).getIdent();

                if (lowFloatInterval > highFloatInterval)
                    throw Store.failException;

                if (lowFloatInterval < MIN_FLOAT) {
                    System.err.println("Minimal value for float variable " + ident + " too low; changed to " + MIN_FLOAT);
                    lowFloatInterval = MIN_FLOAT;
                }
                if (highFloatInterval > MAX_FLOAT) {
                    System.err.println("Maximal value for float variable " + ident + " too high; changed to " + MAX_FLOAT);
                    highFloatInterval = MAX_FLOAT;
                }

                varFloat = new FloatVar(store, ident, lowFloatInterval, highFloatInterval);

                table.addFloatVariable(ident, varFloat);
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {

                    if (constant_float(node, initChild)) {
                        initValFloat = getScalarFlatExprFloat(node, initChild);
                        varFloat.domain.in(store.level, varFloat, initValFloat, initValFloat);
                    } else {
                        initVarFloat = getScalarFlatExpr_varFloat(store, node, initChild);
                        PeqQ c = new PeqQ(varFloat, initVarFloat);
                        pose(store, c);
                    }
                }

                //              if (!is_defined_var || output_var)
                table.addSearchFloatVar(varFloat);
                if (output_var)
                    table.addOutVar(varFloat);
                numberFloatVariables++;
                break;
            default:
                throw new IllegalArgumentException("Not supported type in parameter; compilation aborted.");
        }

    }

    void generateArray(SimpleNode node, Tables table, Store store) {
        if (((ASTVarDeclItem) node).getKind() == 2)
            generateArrayVariables(node, table, store);
        else if (((ASTVarDeclItem) node).getKind() == 3)
            generateArrayParameters(node, table);
        else {
            throw new IllegalArgumentException("Internal error");
        }
    }

    void generateArrayParameters(SimpleNode node, Tables table) {

        dictionary = table;
        annotations = new HashSet<String>();
        //      boolean output_array = false;
        //      OutputArrayAnnotation outArrayAnn=null;

        int type = getType(node);

        int initChild = getAnnotations(node, 1);

        // node.dump("");
        // System.out.println("*** Type = " + type + " init index = " + initChild);
        // System.out.println("*** Annotations: " + annotations);

        String ident = ((ASTVarDeclItem) node).getIdent();

        //      if (annotations.contains("output_array")) {
        //          output_array = true;
        //          outArrayAnn = new OutputArrayAnnotation(ident, indexBounds);
        //      }

        int size;
        int[] val;
        IntDomain[] setValue;
        switch (type) {
            case 0: // array of int
            case 1: // array of int interval
            case 2: // array of int list
            case 3: // array of bool
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                val = getArrayOfScalarFlatExpr(node, initChild, size);
                table.addIntArray(ident, val);
                break;
            case 4: // array of set int
            case 5: // array of set interval
            case 6: // array of set list
            case 7: // array of bool set
            case 10: // array of range set
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                setValue = getSetLiteralArray(node, initChild, size);
                table.addSetArray(ident, setValue);
                break;
            case 8: // array of float
            case 9:
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                double[] valFloat = getArrayOfScalarFlatExprFloat(node, initChild, size);
                table.addFloatArray(ident, valFloat);
                break;
            default:
                throw new IllegalArgumentException("Not supported type in array parameter; compilation aborted.");
        }
    }

    void generateArrayVariables(SimpleNode node, Tables table, Store store) {

        dictionary = table;
        annotations = new HashSet<String>();
        indexBounds = new ArrayList<IntDomain>();
        boolean var_introduced = false;
        boolean output_array = false;
        OutputArrayAnnotation outArrayAnn = null;

        // node.dump("a  ");
        //      System.out.println("*** Type = " + type + " init index = " + initChild);
        //      System.out.println("*** Annotations: " + annotations + "  " + indexBounds);

        int type = getType(node);

        int initChild = getArrayAnnotations(node, 1);

        String ident = ((ASTVarDeclItem) node).getIdent();

        if (annotations.contains("var_is_introduced"))
            var_introduced = true;
        if (annotations.contains("output_array")) {
            output_array = true;
            outArrayAnn = new OutputArrayAnnotation(ident, indexBounds);
        }

        int size;
        IntVar[] varArrayInt;
        FloatVar[] varArrayFloat;
        SetVar[] varArraySet;

        switch (type) {
            case 0: // array of int
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArrayInt = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    varArrayInt = getScalarFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchArray(varArrayInt);
                } else { // no init values
                    varArrayInt = new IntVar[size];
                    for (int i = 0; i < size; i++)
                        varArrayInt[i] = new IntVar(store, ident + "[" + i + "]", MIN_INT, MAX_INT);
                    table.addSearchArray(varArrayInt);
                }
                table.addVariableArray(ident, varArrayInt);
                if (output_array) {
                    outArrayAnn.setArray(varArrayInt);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 1: // array of int interval
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArrayInt = null;

                if (lowInterval > highInterval)
                    throw Store.failException;

                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArrayInt = getScalarFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchArray(varArrayInt);
                } else { // no init values
                    varArrayInt = new IntVar[size];

                    for (int i = 0; i < size; i++)
                        if (interval)
                            varArrayInt[i] = new IntVar(store, ident + "[" + i + "]", new IntervalDomain(lowInterval, highInterval));
                        else
                            varArrayInt[i] = new IntVar(store, ident + "[" + i + "]", lowInterval, highInterval);

                    table.addSearchArray(varArrayInt);
                }
                table.addVariableArray(ident, varArrayInt);
                if (output_array) {
                    outArrayAnn.setArray(varArrayInt);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 2: // array of int list
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArrayInt = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArrayInt = getScalarFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchArray(varArrayInt);
                } else { // no init values
                    varArrayInt = new IntVar[size];
                    for (int i = 0; i < size; i++) {
                        IntervalDomain dom = new IntervalDomain();
                        for (Integer e : intList)
                            dom.unionAdapt(e.intValue(), e.intValue());
                        varArrayInt[i] = new IntVar(store, ident + "[" + i + "]", dom);
                    }
                    table.addSearchArray(varArrayInt);
                }
                table.addVariableArray(ident, varArrayInt);
                if (output_array) {
                    outArrayAnn.setArray(varArrayInt);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 3: // array of bool
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArrayInt = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    varArrayInt = getScalarFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchArray(varArrayInt);
                } else { // no init values
                    varArrayInt = new IntVar[size];
                    for (int i = 0; i < size; i++)
                        varArrayInt[i] = new BooleanVar(store, ident + "[" + i + "]");
                    table.addSearchArray(varArrayInt);
                    numberBooleanVariables += size;
                }
                table.addVariableArray(ident, varArrayInt);
                if (output_array) {
                    outArrayAnn.setArray(varArrayInt);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 4: // array of set int
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArraySet = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArraySet = getSetFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchSetArray(varArraySet);
                } else { // no init values
                    varArraySet = new SetVar[size];
                    for (int i = 0; i < size; i++)
                        varArraySet[i] = new SetVar(store, ident + "[" + i + "]", new BoundSetDomain(MIN_INT, MAX_INT));
                    table.addSearchSetArray(varArraySet);
                    numberSetVariables += size;
                }
                table.addSetVariableArray(ident, varArraySet);
                if (output_array) {
                    outArrayAnn.setArray(varArraySet);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 5: // array of set interval
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArraySet = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArraySet = getSetFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchSetArray(varArraySet);
                } else { // no init values
                    varArraySet = new SetVar[size];
                    for (int i = 0; i < size; i++)
                        if (lowInterval > highInterval)
                            varArraySet[i] = new SetVar(store, ident + "[" + i + "]", new BoundSetDomain());
                        else
                            varArraySet[i] = new SetVar(store, ident + "[" + i + "]",
                                new BoundSetDomain(new IntervalDomain(), new IntervalDomain(lowInterval, highInterval)));
                    table.addSearchSetArray(varArraySet);
                    numberSetVariables += size;
                }
                table.addSetVariableArray(ident, varArraySet);
                if (output_array) {
                    outArrayAnn.setArray(varArraySet);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 6: // array of set list
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArraySet = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArraySet = getSetFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchSetArray(varArraySet);
                } else { // no init values
                    varArraySet = new SetVar[size];
                    for (int i = 0; i < size; i++) {
                        IntDomain sd = new IntervalDomain();
                        for (Integer e : intList)
                            sd.unionAdapt(e.intValue(), e.intValue());
                        varArraySet[i] = new SetVar(store, ident + "[" + i + "]", new BoundSetDomain(new IntervalDomain(), sd));
                    }
                    table.addSearchSetArray(varArraySet);
                    numberSetVariables += size;
                }
                table.addSetVariableArray(ident, varArraySet);
                if (output_array) {
                    outArrayAnn.setArray(varArraySet);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 7: // array of bool set
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArraySet = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArraySet = getSetFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchSetArray(varArraySet);
                } else { // no init values
                    varArraySet = new SetVar[size];
                    for (int i = 0; i < size; i++)
                        varArraySet[i] = new SetVar(store, ident + "[" + i + "]", new BoundSetDomain(0, 1));
                    table.addSearchSetArray(varArraySet);
                    numberSetVariables += size;
                }
                table.addSetVariableArray(ident, varArraySet);
                if (output_array) {
                    outArrayAnn.setArray(varArraySet);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 10: // array of range set
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArraySet = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArraySet = getSetFlatExpr_ArrayVar(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchSetArray(varArraySet);
                } else { // no init values
                    varArraySet = new SetVar[size];
                    for (int i = 0; i < size; i++)
                        varArraySet[i] = new SetVar(store, ident + "[" + i + "]", new BoundSetDomain(new IntervalDomain(), rangeDomain));
                    table.addSearchSetArray(varArraySet);
                    numberSetVariables += size;
                }
                table.addSetVariableArray(ident, varArraySet);
                if (output_array) {
                    outArrayAnn.setArray(varArraySet);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 8: // array of float
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArrayFloat = null;
                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    varArrayFloat = getScalarFlatExpr_ArrayVarFloat(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchFloatArray(varArrayFloat);
                } else { // no init values
                    varArrayFloat = new FloatVar[size];
                    for (int i = 0; i < size; i++)
                        varArrayFloat[i] = new FloatVar(store, ident + "[" + i + "]", MIN_FLOAT, MAX_FLOAT);
                    table.addSearchFloatArray(varArrayFloat);
                    numberFloatVariables += size;
                }
                table.addVariableFloatArray(ident, varArrayFloat);
                if (output_array) {
                    outArrayAnn.setArray(varArrayFloat);
                    table.addOutArray(outArrayAnn);
                }
                break;
            case 9: // array of float interval
                size = ((ASTVarDeclItem) node).getHighIndex() - ((ASTVarDeclItem) node).getLowIndex() + 1;
                varArrayFloat = null;

                if (lowFloatInterval > highFloatInterval)
                    throw Store.failException;

                if (lowFloatInterval < MIN_FLOAT) {
                    System.err.println("Minimal value for array float variable " + ident + " too low; changed to " + MIN_FLOAT);
                    lowFloatInterval = MIN_FLOAT;
                }
                if (highFloatInterval > MAX_FLOAT) {
                    System.err.println("Maximal value for array float variable " + ident + " too high; changed to " + MAX_FLOAT);
                    highFloatInterval = MAX_FLOAT;
                }

                if (initChild < ((ASTVarDeclItem) node).jjtGetNumChildren()) {
                    // array initialization
                    varArrayFloat = getScalarFlatExpr_ArrayVarFloat(store, node, initChild);
                    // if (output_array)
                    //  table.addSearchFloatArray(varArrayFloat);
                } else { // no init values
                    varArrayFloat = new FloatVar[size];

                    for (int i = 0; i < size; i++)
                        varArrayFloat[i] = new FloatVar(store, ident + "[" + i + "]", lowFloatInterval, highFloatInterval);

                    table.addSearchFloatArray(varArrayFloat);
                    numberFloatVariables += size;
                }
                table.addVariableFloatArray(ident, varArrayFloat);
                if (output_array) {
                    outArrayAnn.setArray(varArrayFloat);
                    table.addOutArray(outArrayAnn);
                }
                break;

            default:
                throw new IllegalArgumentException("Not supported type in array parameter; compilation aborted.");
        }
    }


    // 0 - int; 1 - int interval; 2 - int list; 3 - bool; 
    // 4 - set int; 5 - set interval; 6 - set list; 7- bool set;
    // 8 - float; 9 - float interval;
    // 10- range set {|1..n, m, ...|}
    int getType(SimpleNode node) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(0);
        // System.out.println("*** " + child + " value = " + ((SimpleNode)child).jjtGetValue());

        if (child.getId() == JJTINTTIEXPRTAIL) {
            int intType = ((ASTIntTiExprTail) child).getType();
            switch (intType) {
                case 0: // int
                    break;
                case 1: // int interval
                    lowInterval = ((ASTIntTiExprTail) child).getLow();
                    highInterval = ((ASTIntTiExprTail) child).getHigh();
                    if (checkBounds)
                        if (lowInterval < IntDomain.MinInt || highInterval > IntDomain.MaxInt)
                            throw new ArithmeticException("Too large bounds on intervals " + lowInterval + ".." + highInterval);
                    break;
                case 2: // int list
                    SimpleNode grand_child = (SimpleNode) child.jjtGetChild(0);
                    intList = ((ASTIntLiterals) grand_child).getList();
                    if (checkBounds)
                        for (Integer e : intList)
                            if (e.intValue() < IntDomain.MinInt || e.intValue() > IntDomain.MaxInt)
                                throw new ArithmeticException("Too large element in set " + e.intValue());
                    break;
                default:
                    throw new RuntimeException("Internal error in " + getClass().getName());
            }
            return intType;
        } else if (child.getId() == JJTBOOLTIEXPRTAIL)
            return 3;
        else if (child.getId() == JJTSETTIEXPRTAIL) {
            SimpleNode grand_child = (SimpleNode) child.jjtGetChild(0);
            if (grand_child.getId() == JJTINTTIEXPRTAIL) {

                int intType = ((ASTIntTiExprTail) grand_child).getType();
                switch (intType) {
                    case 0: // int
                        break;
                    case 1: // int interval
                        lowInterval = ((ASTIntTiExprTail) grand_child).getLow();
                        highInterval = ((ASTIntTiExprTail) grand_child).getHigh();
                        if (checkBounds)
                            if (lowInterval < IntDomain.MinInt || highInterval > IntDomain.MaxInt)
                                throw new ArithmeticException("Too large bounds on intervals " + lowInterval + ".." + highInterval);
                        break;
                    case 2: // int list
                        SimpleNode grand_grand_child = (SimpleNode) grand_child.jjtGetChild(0);
                        intList = ((ASTIntLiterals) grand_grand_child).getList();
                        if (checkBounds)
                            for (Integer e : intList)
                                if (e.intValue() < IntDomain.MinInt || e.intValue() > IntDomain.MaxInt)
                                    throw new ArithmeticException("Too large element in set " + e.intValue());
                        break;
                    case 3: // range set
                        rangeDomain = new IntervalDomain();
                        int n = grand_child.jjtGetNumChildren();
                        for (int i = 0; i < n; i++) {
                            SimpleNode setElement = (SimpleNode) grand_child.jjtGetChild(i);
                            if (setElement.getId() == JJTSETELEMENT) {
                                SimpleNode e1 = (SimpleNode) setElement.jjtGetChild(0);
                                if (e1.getId() == JJTSCALARFLATEXPR) {
                                    rangeDomain.unionAdapt(((ASTScalarFlatExpr) e1).getInt());
                                } else if (e1.getId() == JJTINTFLATEXPR) {
                                    SimpleNode e2 = (SimpleNode) setElement.jjtGetChild(1);
                                    rangeDomain.unionAdapt(new Interval(((ASTIntFlatExpr) e1).getInt(), ((ASTIntFlatExpr) e2).getInt()));
                                }
                            }
                        }
                        return 10;
                    default:
                        throw new RuntimeException("Internal error in " + getClass().getName());
                }
                //              return ((ASTIntTiExprTail)grand_child).getType()+4;
                return intType + 4;
            } else if (grand_child.getId() == JJTBOOLTIEXPRTAIL)
                return 7;
            else
                return -1;
        } else if (child.getId() == JJTFLOATTIEXPRTAIL) {
            int doubleType = ((ASTFloatTiExprTail) child).getType();
            switch (doubleType) {
                case 0: // float
                    break;
                case 1: // float interval
                    lowFloatInterval = ((ASTFloatTiExprTail) child).getLow();
                    highFloatInterval = ((ASTFloatTiExprTail) child).getHigh();
                    break;
                default:
                    throw new RuntimeException("Internal error in " + getClass().getName());
            }
            // System.out.println ("returns double type " + (int)(doubleType + 8));

            return (doubleType + 8);
        } else
            return -1;
    }

    int getAnnotations(SimpleNode node, int i) {
        int j = i;
        int count = node.jjtGetNumChildren();
        if (j < count) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(j);
            while (j < count && child.getId() == JJTANNOTATION) {
                SimpleNode grandchild = (SimpleNode) child.jjtGetChild(0);

                if (grandchild.getId() == JJTANNEXPR)
                    annotations.add(parseAnnExpr(grandchild, 0));

                j++;
                if (j < count)
                    child = (SimpleNode) node.jjtGetChild(j);
            }
        }
        // System.out.println(annotations);

        return j;
    }

    String parseAnnExpr(SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 2: // ident
                    return ((ASTScalarFlatExpr) child).getIdent();
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in annotation; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported annotation type; compilation aborted.");
        }
    }
    
    int getArrayAnnotations(SimpleNode node, int i) {
        // node.dump("output_array   ");

        int j = i;
        int count = node.jjtGetNumChildren();

        if (j < count) {
            SimpleNode child = (SimpleNode) node.jjtGetChild(j);
            while (j < count && child.getId() == JJTANNOTATION) {
                // child.dump("ch  ");
                String id = ((ASTAnnotation) child).getAnnId();

                if (id != null && id.equals("output_array")) {
                    annotations.add(id);

                    child = (SimpleNode) child.jjtGetChild(0);

                    int noAnnotations = child.jjtGetNumChildren();
                    // System.out.println("children = " + noAnnotations);
                    for (int nc = 0; nc < noAnnotations; nc++) {

                        // System.out.println("=====================>");

                        SimpleNode nchild = (SimpleNode) child.jjtGetChild(nc);
                        int no = nchild.jjtGetNumChildren();
                        // nchild.dump("g  ");

                        if (no > 1 || ((SimpleNode) nchild.jjtGetChild(0)).getId() != JJTANNEXPR) {
                            throw new IllegalArgumentException(
                                                               "More than one annotation expression in output_array annotation; execution aborted");
                        } else {
                            SimpleNode grandchild = (SimpleNode) nchild.jjtGetChild(0);
                            int number = grandchild.jjtGetNumChildren();
                            if (number == 1) {
                                SimpleNode setLiteral = (SimpleNode) grandchild.jjtGetChild(0);
                                if (setLiteral.getId() == JJTSETLITERAL) {

                                    // int numberSL = arrayLiteral.jjtGetNumChildren();
                                    // for (int p = 0; p < numberSL; p++) {
                                    //  SimpleNode setLiteral = (SimpleNode) arrayLiteral.jjtGetChild(p);
                                    if (((ASTSetLiteral) setLiteral).getType() == 0) { // interval
                                        int s_n = setLiteral.jjtGetNumChildren();
                                        if (s_n == 2) {
                                            int low = ((ASTIntFlatExpr) setLiteral.jjtGetChild(0)).getInt();
                                            int high = ((ASTIntFlatExpr) setLiteral.jjtGetChild(1)).getInt();
                                            IntDomain indexes = new IntervalDomain(low, high);
                                            indexBounds.add(indexes);
                                            //System.out.println(indexes+"->"+indexes.min() +"__"+indexes.max());
                                        } else {
                                            throw new IllegalArgumentException(
                                                                    "Unexpected set literal in output_array annotation; execution aborted");
                                        }
                                    } else if (((ASTSetLiteral) setLiteral).getType() == 1) { // list
                                        int s_n = setLiteral.jjtGetNumChildren();
                                        IntDomain indexes = new IntervalDomain();
                                        for (int k = 0; k < s_n; k++) {
                                            int el = ((ASTScalarFlatExpr) setLiteral.jjtGetChild(k)).getInt();
                                            indexes.unionAdapt(el);
                                        }
                                        indexBounds.add(indexes);
                                    } else {
                                        throw new IllegalArgumentException(
                                                                           "Unexpected set literal in output_array annotation; execution aborted");
                                    }
                                    // }
                                } else {
                                    throw new IllegalArgumentException("Wrong expression in output_array annotation; execution aborted");
                                }
                            }
                        }
                        // System.out.println("<=====================");

                    }
                    j++;
                    if (j < count)
                        child = (SimpleNode) node.jjtGetChild(j);
                } else {
                    // simple annotation id
                    annotations.add(parseAnnExpr((SimpleNode)child.jjtGetChild(0), 0));
                    j++;
                    if (j < count)
                        child = (SimpleNode) node.jjtGetChild(j);
                }
            }
        }
        // System.out.println(annotations +", "+indexBounds);

        return j;
    }

    boolean constant_int(SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 0: // int
                case 1: // bool
                    return true;
                case 2: // ident
                    Integer n = dictionary.checkInt(((ASTScalarFlatExpr) child).getIdent());
                    if (n != null)
                        return true;
                    else
                        return false;
                case 3: // array acces
                    int[] an = dictionary.getIntArray(((ASTScalarFlatExpr) child).getIdent());
                    if (an != null)
                        return true;
                    else
                        return false;
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
    }

    boolean constant_float(SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);

        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                // case 0: // int
                // case 1: // bool
                //      return true;
                case 2: // ident
                    Double n = dictionary.checkFloat(((ASTScalarFlatExpr) child).getIdent());
                    if (n != null)
                        return true;
                    else
                        return false;
                case 3: // array acces
                    double[] an = dictionary.getFloatArray(((ASTScalarFlatExpr) child).getIdent());
                    if (an != null)
                        return true;
                    else
                        return false;
                case 5:  // float
                    return true;
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
    }

    boolean constant_set(SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSETLITERAL) {  //SCALARFLATEXPR) {
            switch (((ASTSetLiteral) child).getType()) {
                case 0: // interval
                case 1: // list
                case 2: // range set
                    return true;
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 2: // ident
                    IntDomain n = dictionary.getSet(((ASTScalarFlatExpr) child).getIdent());
                    if (n != null)
                        return true;
                    else
                        return false;
                case 3: // array acces
                    IntDomain[] an = dictionary.getSetArray(((ASTScalarFlatExpr) child).getIdent());
                    if (an != null)
                        return true;
                    else
                        return false;
                default: // int, bool, string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
    }

    int getScalarFlatExpr(SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 0: // int
                    return ((ASTScalarFlatExpr) child).getInt();
                case 1: // bool
                    return ((ASTScalarFlatExpr) child).getInt();
                case 2: // ident
                    return dictionary.getInt(((ASTScalarFlatExpr) child).getIdent());
                case 3: // array acces
                    return dictionary.getIntArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
    }

    double getScalarFlatExprFloat(SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);

        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 5: // float
                    return ((ASTScalarFlatExpr) child).getFloat();
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
    }

    IntVar[] getScalarFlatExpr_ArrayVar(Store store, SimpleNode node, int index) {

        SimpleNode child = (SimpleNode) node.jjtGetChild(index);
        if (child.getId() == JJTARRAYLITERAL) {
            int count = child.jjtGetNumChildren();
            IntVar[] av = new IntVar[count];
            //      System.out.println(child + " count = " + count);
            for (int i = 0; i < count; i++) {
                av[i] = getScalarFlatExpr_var(store, child, i);
            }
            return av;
        } else {
            throw new IllegalArgumentException("Expeceted array literal, found " + child.getId() + " ; compilation aborted.");
        }
    }

    FloatVar[] getScalarFlatExpr_ArrayVarFloat(Store store, SimpleNode node, int index) {

        SimpleNode child = (SimpleNode) node.jjtGetChild(index);
        if (child.getId() == JJTARRAYLITERAL) {
            int count = child.jjtGetNumChildren();
            FloatVar[] av = new FloatVar[count];
            //      System.out.println(child + " count = " + count);
            for (int i = 0; i < count; i++) {
                av[i] = getScalarFlatExpr_varFloat(store, child, i);
            }
            return av;
        } else {
            throw new IllegalArgumentException("Expeceted array literal, found " + child.getId() + " ; compilation aborted.");
        }
    }


    IntVar getScalarFlatExpr_var(Store store, SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 0: // int
                    int val = ((ASTScalarFlatExpr) child).getInt();
                    return dictionary.getConstant(val); // new IntVar(store, val, val);
                case 1: // bool
                    val = ((ASTScalarFlatExpr) child).getInt();
                    BooleanVar bb = dictionary.getConstantBoolean(val); //new BooleanVar(store, val, val);
                    //numberBooleanVariables++; // not really a variable; constant
                    return bb;
                case 2: // ident
                    IntVar var = dictionary.getVariable(((ASTScalarFlatExpr) child).getIdent());
                    if (var != null)
                        return var;
                    else {
                        Integer n = dictionary.getInt(((ASTScalarFlatExpr) child).getIdent());
                        if (n != null)
                            return dictionary.getConstant(n.intValue()); // new IntVar(store, n.intValue(), n.intValue());
                        else
                            break;
                    }
                case 3: // array acces
                    IntVar avar = dictionary.getVariableArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                    if (avar != null)
                        return avar;
                    else {
                        Integer an = dictionary.getIntArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                        if (an != null)
                            return dictionary.getConstant(an.intValue()); // new IntVar(store, an.intValue(), an.intValue());
                        else
                            break;
                    }
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
        return new IntVar(store);
    }

    FloatVar getScalarFlatExpr_varFloat(Store store, SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                // case 0: // int
                //      return new IntVar(store, ((ASTScalarFlatExpr)child).getInt(), ((ASTScalarFlatExpr)child).getInt());
                // case 1: // bool
                //      BoundDomain d = new BoundDomain(((ASTScalarFlatExpr)child).getInt(), ((ASTScalarFlatExpr)child).getInt());
                //      BooleanVar bb = new BooleanVar(store,"",d);
                //      //numberBooleanVariables++; // not really a variable; constant
                //      return bb;
                case 2: // ident
                    FloatVar var = dictionary.getFloatVariable(((ASTScalarFlatExpr) child).getIdent());
                    if (var != null)
                        return var;
                    else {
                        Double n = dictionary.getFloat(((ASTScalarFlatExpr) child).getIdent());
                        if (n != null)
                            return dictionary.getFloatConstant(n.doubleValue()); //new FloatVar(store, n.doubleValue(), n.doubleValue());
                        else
                            break;
                    }
                case 3: // array acces
                    FloatVar avar =
                        dictionary.getVariableFloatArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                    if (avar != null)
                        return avar;
                    else {
                        Double an = dictionary.getFloatArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                        if (an != null)
                            return dictionary.getFloatConstant(an.doubleValue()); //new FloatVar(store, an.doubleValue(), an.doubleValue());
                        else
                            break;
                    }
                case 5: // float
                    return dictionary.getFloatConstant(((ASTScalarFlatExpr) child).getFloat());
                default: // string & float;
                    throw new IllegalArgumentException("Not supported scalar in parameter; compilation aborted.");
            }
        } else {
            throw new IllegalArgumentException("Not supported parameter assignment; compilation aborted.");
        }
        return new FloatVar(store);
    }


    SetVar[] getSetFlatExpr_ArrayVar(Store store, SimpleNode node, int index) {

        SimpleNode child = (SimpleNode) node.jjtGetChild(index);

        if (child.getId() == JJTARRAYLITERAL) {
            int count = child.jjtGetNumChildren();
            SetVar[] av = new SetVar[count];
            //System.out.println(child + " count = " + count);
            for (int i = 0; i < count; i++) {
                av[i] = (SetVar) getSetFlatExpr_var(store, child, i);
            }
            return av;
        } else {
            throw new IllegalArgumentException("Expeceted array literal, found " + child.getId() + " ; compilation aborted.");
        }
    }

    SetVar getSetFlatExpr_var(Store store, SimpleNode node, int i) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(i);
        if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 2: // ident
                    SetVar var = dictionary.getSetVariable(((ASTScalarFlatExpr) child).getIdent());
                    if (var != null)
                        return var;
                    else {
                        IntDomain n = dictionary.getSet(((ASTScalarFlatExpr) child).getIdent());
                        if (n != null)
                            // FIXME, why do we create int var inside this function? -- FIXED by KK
                            return new SetVar(store, new BoundSetDomain(n, n));
                        else
                            break;
                    }
                case 3: // array acces
                    SetVar avar =
                        dictionary.getSetVariableArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                    if (avar != null)
                        return avar;
                    else {
                        IntDomain an = dictionary.getSetArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                        if (an != null)
                            return new SetVar(store, new BoundSetDomain(an, an));
                        else
                            break;
                    }
                default: // string & float;
                    throw new IllegalArgumentException(
                        "Not supported scalar in parameter " + ((ASTScalarFlatExpr) child).getIdent() + "; compilation aborted.");
            }
        } else if (child.getId() == JJTSETLITERAL) {
            IntDomain s = getSetLiteral(node, i);
            SetVar setVar = new SetVar(store, new BoundSetDomain(s, s));
            return setVar;
        }
        throw new IllegalArgumentException(
            "Not supported parameter assignment " + ((ASTScalarFlatExpr) child).getIdent() + "; compilation aborted.");
    }

    int[] getArrayOfScalarFlatExpr(SimpleNode node, int index, int size) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(index);
        int count = child.jjtGetNumChildren();
        if (count == size) {
            int[] aa = new int[size];
            for (int i = 0; i < count; i++) {
                int el = getScalarFlatExpr(child, i);
                aa[i] = el;
            }
            return aa;
        } else {
            throw new IllegalArgumentException("Different size declaration and intiallization of int array; compilation aborted.");
        }
    }

    double[] getArrayOfScalarFlatExprFloat(SimpleNode node, int index, int size) {
        SimpleNode child = (SimpleNode) node.jjtGetChild(index);
        int count = child.jjtGetNumChildren();
        if (count == size) {
            double[] aa = new double[size];
            for (int i = 0; i < count; i++) {
                double el = getScalarFlatExprFloat(child, i);
                aa[i] = el;
            }
            return aa;
        } else {
            throw new IllegalArgumentException("Different size declaration and intiallization of int array; compilation aborted.");
        }
    }


    IntDomain getSetLiteral(SimpleNode node, int index) {
        // node.dump("VariableParameters.getSetLiteral ");
        SimpleNode child = (SimpleNode) node.jjtGetChild(index);
        if (child.getId() == JJTSETLITERAL) {
            switch (((ASTSetLiteral) child).getType()) {
                case 0: // interval
                    SimpleNode grand_child_1 = (SimpleNode) child.jjtGetChild(0);
                    SimpleNode grand_child_2 = (SimpleNode) child.jjtGetChild(1);
                    if (grand_child_1.getId() == JJTINTFLATEXPR && grand_child_2.getId() == JJTINTFLATEXPR) {
                        int i1 = ((ASTIntFlatExpr) grand_child_1).getInt();
                        int i2 = ((ASTIntFlatExpr) grand_child_2).getInt();
                        if (i1 > i2)
                            return new IntervalDomain();
                        else
                            return new IntervalDomain(i1, i2);
                    }
                    break;
                case 1: // list
                    IntDomain s = new IntervalDomain();
                    int el = -1111;
                    int count = child.jjtGetNumChildren();
                    for (int i = 0; i < count; i++) {
                        el = getScalarFlatExpr(child, i);
                        s.unionAdapt(el);
                    }
                    return s;
                case 2: // range
                    IntDomain d = new IntervalDomain();
                    int n = child.jjtGetNumChildren();
                    for (int i = 0; i < n; i++) {
                        SimpleNode setElement = (SimpleNode) child.jjtGetChild(i);
                        if (setElement.getId() == JJTSETELEMENT) {
                            SimpleNode e1 = (SimpleNode) setElement.jjtGetChild(0);
                            if (e1.getId() == JJTSCALARFLATEXPR) {
                                d.unionAdapt(((ASTScalarFlatExpr) e1).getInt());
                            } else if (e1.getId() == JJTINTFLATEXPR) {
                                SimpleNode e2 = (SimpleNode) setElement.jjtGetChild(1);
                                d.unionAdapt(new Interval(((ASTIntFlatExpr) e1).getInt(), ((ASTIntFlatExpr) e2).getInt()));
                            }
                        }
                    }
                    return d;
                default:
                    throw new IllegalArgumentException("Set type not supported; compilation aborted.");
            }
        } else if (child.getId() == JJTSCALARFLATEXPR) {
            switch (((ASTScalarFlatExpr) child).getType()) {
                case 0: // int
                case 1: // bool
                    throw new IllegalArgumentException("Set initialization fault; compilation aborted.");
                case 2: // ident
                    return dictionary.getSet(((ASTScalarFlatExpr) child).getIdent());
                case 3: // array access
                    return dictionary.getSetArray(((ASTScalarFlatExpr) child).getIdent())[((ASTScalarFlatExpr) child).getInt()];
                case 4: // string
                case 5: // float
                    throw new IllegalArgumentException("Set initialization fault; compilation aborted.");
                default:
                    throw new IllegalArgumentException("Set initialization fault; compilation aborted.");
            }
        }
        return new IntervalDomain();
    }

    IntDomain[] getSetLiteralArray(SimpleNode node, int index, int size) {
        IntDomain[] s = new IntDomain[size];
        int arrayIndex = 0;

        SimpleNode child = (SimpleNode) node.jjtGetChild(index);
        if (child.getId() == JJTARRAYLITERAL) {
            int count = child.jjtGetNumChildren();
            if (count == size) {
                for (int i = 0; i < count; i++) {
                    s[arrayIndex++] = getSetLiteral(child, i);
                }
            } else {
                throw new IllegalArgumentException("Different array sizes in specification and initialization; compilation aborted.");
            }
        }
        return s;
    }

    boolean ground(Var v) {

        return v.singleton();

    }

    void pose(Store store, Constraint c) throws FailException {

        store.imposeWithConsistency(c);

        if (options.debug())
            System.out.println("% " + c);
    }
}
