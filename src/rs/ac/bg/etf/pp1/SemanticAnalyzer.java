package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.ac.bg.etf.pp1.util.CommonUtils;

public class SemanticAnalyzer extends VisitorAdaptor {
    /* Class fields */
    private Obj currentMethod = null;

    private final List<ConstAssignment> currentConstAssignments = new ArrayList<>();
    private final List<VarDesignatorNoError> currentVars = new ArrayList<>();
    private LastVarDesignatorNoError currentLastVar = null;

    private boolean mainFound = false;
    private boolean returnFound = false;

    private int numberOfVariables = 0;

    /* Printing and logging */
    private static final Logger log = Logger.getLogger(SemanticAnalyzer.class);
    private boolean errorDetected = false;

    public void report_error(String message, SyntaxNode info) {
        errorDetected = true;

        StringBuilder msg = new StringBuilder("Semantic error: " + message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" : at line ").append(line);
        log.error(msg.toString());
    }

    public void report_info(String message, SyntaxNode info) {
        StringBuilder msg = new StringBuilder(message);
        int line = (info == null) ? 0 : info.getLine();
        if (line != 0)
            msg.append(" : at line ").append(line);
        log.info(msg.toString());
    }

    /* ---------> 0. Productions related to the program <--------- */

    /**
     * This method adds an object node for the program to the symbol table.
     * It also saves the object node for later use and opens a new scope.
     * <p>
     * At the end of the program (by calling visit method for Program), the saved object node
     * will be used to link local symbols and close the scope.
     */
    @Override
    public void visit(ProgName progName) {
        // Insert the program object node into the symbol table and open a new scope
        progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
        Tab.openScope();
    }

    /**
     * This method finishes the program by:
     * - checking if the main method exists,
     * - defining the number of existing variables in the program's scope,
     * - chaining local variables to the object node for this program,
     * - and closing the scope.
     */
    @Override
    public void visit(Program program) {
        // Check if the main method exists
        if (!mainFound) {
            report_error("Error: Method 'void main() { ... }' is not defined!", null);
        }
        this.numberOfVariables = Tab.currentScope.getnVars();

        // Chain local symbols to the object node for this program
        Tab.chainLocalSymbols(program.getProgName().obj);

        // Close the scope
        Tab.closeScope();
        report_info("Program finished. Scope closed.", null);
    }

    /* ---------> 1. Productions related to constant declarations <--------- */

    /**
     * Put into symbol table global named constants.
     * - Check if a constant with the same name exists in the symbol table.
     * - Compare the expected type of the constant according to the declaration and the type of the initializer.
     * - At the end of 'ConstDecl.visit' traversal, empty 'currentConstAssignments'.
     */
    @Override
    public void visit(ConstDecl constDecl) {
        for (ConstAssignment constAssignment : currentConstAssignments) {
            // Determine the type and value of the initializer
            Struct initializerType = constAssignment.getConst().struct;
            int initializerValue = CommonUtils.getConstValue(constAssignment.getConst());

            // Check if the name already exists in the symbol table
            if (Tab.find(constAssignment.getName()) != Tab.noObj) {
                report_error("Constant " + constAssignment.getName() + " is already declared in the current scope", constDecl);
                constAssignment.obj = Tab.noObj;
            } else if (!initializerType.assignableTo(constDecl.getType().struct)) { // Compare the type of the constant and the initializer
                report_error("Constant " + constAssignment.getName() + " is not of the same type as the initializer", constDecl);
                constAssignment.obj = Tab.noObj;
            } else { // Write the constant into the symbol table
                constAssignment.obj = CommonUtils.addConstToSymTable(constAssignment.getName(), constDecl.getType().struct, initializerValue);
                report_info("Constant declared: " + constAssignment.getName(), constAssignment);
            }
        }

        currentConstAssignments.clear();
    }

    /**
     * Save all visited constant creations.
     * Further processing will be done in the method 'ConstDecl.visit'.
     */
    @Override
    public void visit(ConstAssignment constAssignment) {
        currentConstAssignments.add(constAssignment);
    }

    /* ---------> 2. Productions related to variable declarations <--------- */

    /**
     * Add local and global variables to the symbol table.
     * <p>
     * This concerns the production 'VarDecl' [(VarDecl) VarDeclType VarDesignatorList].
     * For example (e.g., int a, b, c;):
     * * VarDeclType::visit - before this, the variable type is globally saved when visiting Type::visit,
     * * ArrayBrackets::visit - 'isVarArray' needs to be marked as true globally,
     * <p>
     * * (Last)VarDesignatorNoError::visit - the variable name is stored in the symbol table,
     * * VarDecl::visit - since this is visited at the end, 'currentVarType' is reset to null.
     */
    @Override
    public void visit(VarDecl varDecl) {
        String varName;
        boolean isArray;

        // Add variables' names to symbol table (consider whether it's an array)
        for (VarDesignatorNoError varDesignator : currentVars) {
            varName = varDesignator.getVarName();
            isArray = varDesignator.getArraySquareBrackets().getClass() == ArrayBrackets.class;

            addVarToSymTable(varName, isArray, varDecl);
        }

        if (currentLastVar != null) {
            varName = currentLastVar.getVarName();
            isArray = currentLastVar.getArraySquareBrackets().getClass() == ArrayBrackets.class;

            addVarToSymTable(varName, isArray, varDecl);
        }

        currentVars.clear();
        currentLastVar = null;
    }

    private void addVarToSymTable(String varName, boolean isArray, VarDecl varDecl) {
        // Check if the variable name already exists in the same scope
        if (Tab.find(varName) != Tab.noObj && Tab.currentScope().findSymbol(varName) != null) {
            report_error("Variable " + varName + " is already declared in the current scope", varDecl);
        } else {
            // Insert variable into symbol table (consider whether it's an array)
            if (isArray) {
                Tab.insert(Obj.Var, varName, CommonUtils.getArrayType(varDecl.getType().struct));
            } else {
                Tab.insert(Obj.Var, varName, varDecl.getType().struct);
            }
            report_info("Variable declared: " + varName, varDecl);
        }
    }

    /**
     * Save all visited variable names.
     * Further processing will be done in the 'VarDecl.visit' method.
     */
    @Override
    public void visit(VarDesignatorNoError varDesignator) {
        currentVars.add(varDesignator);
    }

    /**
     * The 'LastVar' variable is visited only once (at the end of declaration).
     * However, we will add it to this class' field, and process it in 'VarDecl.visit'.
     */
    @Override
    public void visit(LastVarDesignatorNoError varDesignator) {
        currentLastVar = varDesignator;
    }

    /**
     * Perform a check if it's the correct type in the language.
     * Save the type in the 'Type.struct' field.
     */
    @Override
    public void visit(Type type) {
        Obj typeObj = Tab.find(type.getTypeName());

        // Check if the given type name exists in the symbol table
        if (typeObj == Tab.noObj) {
            report_error("Name " + type.getTypeName() + " does not exist as a type in the language", type);
            type.struct = Tab.noType;
        } else if (typeObj.getKind() == Obj.Type) { // Check if it's a type in the language
            type.struct = typeObj.getType();
        } else {
            report_error("Name " + type.getTypeName() + " exists as a symbol but is not a type in the language", type);
            type.struct = Tab.noType;
        }
    }

    /* ---------> 3. Productions related to method declarations <--------- */

    /**
     * Upon encountering a function declaration (MethodTypeName):
     * - Add 'main' to the symbol table.
     * - Open a scope for the method.
     * <p>
     * At the end of the declaration (in MethodDecl.visit method) we should:
     * - chain symbols (that's why we remember the object node of this method here)
     * - close the scope of this method.
     */
    @Override
    public void visit(MethodTypeName methodTypeName) {
        // Check if the same name already exists in the symbol table
        if (Tab.find(methodTypeName.getName()) != Tab.noObj) { // It does
            // Check if there's already a method and an object with the same name in the same scope
            if (Tab.currentScope().findSymbol(methodTypeName.getName()) != null) {
                report_error("Error at " + methodTypeName.getLine()
                        + ": Method name " + methodTypeName.getName()
                        + " is already declared in the same scope!", null
                );

                // Create the method object node and open the scope
                currentMethod = methodTypeName.obj = Tab.noObj;
                Tab.openScope();
                return;
            }
        }

        // Otherwise, create the method object node and open the scope
        if (methodTypeName.getMethodType().getClass() == MethodReturnType.class) { // Type: int, char, bool
            Struct methodReturnType = ((MethodReturnType) methodTypeName.getMethodType()).getType().struct;
            currentMethod = methodTypeName.obj = Tab.insert(Obj.Meth, methodTypeName.getName(), methodReturnType);
        } else { // Type: VOID
            currentMethod = methodTypeName.obj = Tab.insert(Obj.Meth, methodTypeName.getName(), Tab.noType);
        }

        // Open a scope for the method
        Tab.openScope();
        report_info("Function " + methodTypeName.getName() + " processing started", methodTypeName);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // Check if the method has a return statement (if not - void type)
        if (!returnFound && currentMethod.getType() != Tab.noType) {
            report_error("Error! Method lacks a return statement", methodDecl);
        }

        // Check if it's a 'void main() { ... }' method. We don't consider 'void main(String[] args) { ... }'
        if (currentMethod.getType() == Tab.noType && "main".equals(currentMethod.getName())) {
            mainFound = true;
        }

        // Chain local symbols (into the 'locals' field of the object node of the method) and close the scope
        Tab.chainLocalSymbols(currentMethod);
        Tab.closeScope();

        currentMethod = null;
        returnFound = false;
    }

    /* ---------> 4. Commands in the language: Print, Return, Read <--------- */

    /**
     * Check contextual conditions of the argument type (int, char, bool) during PRINT call.
     */
    @Override
    public void visit(StatementPrint printStmt) {
        Struct argStruct = printStmt.getExpr().struct;

        if (!(argStruct == Tab.intType || argStruct == Tab.charType || argStruct == CommonUtils.boolType)) {
            report_error("Error at " + printStmt.getLine() + ": argument in print call must be of type int, char, or bool", null);
        }
    }

    /**
     * Check contextual conditions for equality of return command type and method return type:
     * - if the method is void and has an expression in the return command - error.
     * - if the method is non-void and has no expression in the return command - error.
     */
    @Override
    public void visit(StatementReturn returnStmt) {
        returnFound = true;

        Struct currentMethodReturnType = currentMethod.getType();
        boolean isReturnEmpty = returnStmt.getReturnParam().getClass() == ReturnEmpty.class;

        // Check if it is a void method returning something (error)
        if (currentMethodReturnType == Tab.noType && !isReturnEmpty) {
            report_error("Return statement of void method " + currentMethod.getName() + " must not contain an expression", returnStmt);
            return;
        }

        // Check if it is a non-void method returning nothing (error)
        if (currentMethodReturnType != Tab.noType && isReturnEmpty) {
            report_error("Method " + currentMethod.getName() + " must have a non-empty return statement", returnStmt);
            return;
        }

        // Check compatibility of return value type and return statement type
        if (currentMethodReturnType != Tab.noType) {
            Struct returnStmtType = ((ReturnExpr) returnStmt.getReturnParam()).getExpr().struct;
            if (!currentMethodReturnType.compatibleWith(returnStmtType)) {
                report_error("Return statement type does not match method type " + currentMethod.getName(), returnStmt);
            }
        }
    }

    @Override
    public void visit(StatementRead statementRead) { // Method 'read(x)'
        // Check if the argument is a variable, Obj.Fld, or array element
        int argKind = statementRead.getDesignator().obj.getKind();
        switch (argKind) {
            case Obj.Var:
            case Obj.Elem:
                break;
            default:
                report_error("Invalid argument for read method", statementRead);
        }

        // Check if the argument type is int, char, or bool
        Struct argType = statementRead.getDesignator().obj.getType();
        if (!(argType == Tab.intType || argType == Tab.charType || argType == CommonUtils.boolType))
            report_error("Argument in read call can only be of type int, char, or bool", statementRead);
    }

    /* ---------> 5. Productions related to Designator and DesignatorStatement <--------- */

    @Override
    public void visit(DesignatorInc designatorInc) {
        // Check if the destination operand is a variable, Obj.Fld, or array element
        int dstKind = designatorInc.getDesignator().obj.getKind();
        switch (dstKind) {
            case Obj.Var:
            case Obj.Elem:
                break;
            default:
                report_error("Invalid destination operand for increment", designatorInc);
        }

        // Check if the destination operand is of type int
        Struct dstType = designatorInc.getDesignator().obj.getType();
        if (dstType != Tab.intType) {
            report_error("Destination operand for increment is not of type int", designatorInc);
        }
    }

    @Override
    public void visit(DesignatorDec designatorDec) {
        int dstKind = designatorDec.getDesignator().obj.getKind();
        switch (dstKind) {
            case Obj.Var:
            case Obj.Elem:
                break;
            default:
                report_error("Invalid destination operand for decrement", designatorDec);
        }

        // Check if the destination operand is of type int
        Struct dstType = designatorDec.getDesignator().obj.getType();
        if (dstType != Tab.intType) {
            report_error("Destination operand for decrement is not of type int", designatorDec);
        }
    }

    @Override
    public void visit(Assignment assignment) {
        // Check if the destination operand is a variable or array element
        int dstKind = assignment.getDesignator().obj.getKind();
        switch (dstKind) {
            case Obj.Var:
            case Obj.Elem:
                break;
            default:
                report_error("Invalid destination operand for value assignment", assignment);
        }

        Struct srcStruct = assignment.getAssignmentValue().struct;
        Struct dstStruct = assignment.getDesignator().obj.getType();

        // Check contextual conditions for value assignment
        if (!srcStruct.assignableTo(dstStruct)) {
            report_error("Incompatible types in value assignment", assignment);
        }
    }

    @Override
    public void visit(AssignmentValueExpr assignmentValue) {
        assignmentValue.struct = assignmentValue.getExpr().struct;
    }

    /**
     * Check for the call 'varName.findAny(value)':
     * - whether varName is indeed an array name,
     * - whether value is comparable with array elements.
     */
    @Override
    public void visit(AssignmentValueFindAny assignmentValue) {
        assignmentValue.struct = CommonUtils.boolType;

        // Check if 'varName' is an array name
        Struct varNameType = assignmentValue.getDesignator().obj.getType();
        if (varNameType.getKind() != Struct.Array) {
            report_error("Function findAny is called only on arrays", assignmentValue);
            return;
        }

        // Check if 'value' is of array elements type
        Struct valueType = assignmentValue.getExpr().struct;
        Struct arrayElemType = varNameType.getElemType();
        if (!valueType.compatibleWith(arrayElemType)) {
            report_error("Array element type does not match the expression type", assignmentValue);
        }
    }

    /**
     * Check for the call 'varName.findAll(value)':
     * - whether varName is indeed an array name,
     * - whether value is comparable with array elements.
     */
    @Override
    public void visit(AssignmentValueFindAll assignmentValue) {
        assignmentValue.struct = Tab.intType;

        // Check if 'varName' is an array name
        Struct varNameType = assignmentValue.getDesignator().obj.getType();
        if (varNameType.getKind() != Struct.Array) {
            report_error("Function findAll is called only on arrays", assignmentValue);
            return;
        }

        // Check if 'value' is of array elements type
        Struct valueType = assignmentValue.getExpr().struct;
        Struct arrayElemType = varNameType.getElemType();
        if (!valueType.compatibleWith(arrayElemType)) {
            report_error("Array element type does not match the expression type", assignmentValue);
        }
    }

    /**
     * Detect the usage of Designator (variable name, array element, or function) in expressions [LEVEL B].
     * <p>
     * Check if the variable name exists in the symbol table.
     * Check if [] are used in the expression, indicating an array.
     * - Check if the variable with [] is an array variable.
     * - Check if the array index is of type int.
     * - Create an Obj node for the used name, which is a variable or array element.
     */
    @Override
    public void visit(Designator designator) {
        String varName = designator.getName();
        boolean isArray = designator.getDesignatorArrayIndex().getClass() == ArrayIndex.class;

        // Check if the given name (used in the expression) exists in the symbol table
        if ((designator.obj = Tab.find(varName)) == Tab.noObj) {
            report_error("Name " + varName + " is not declared", null);
            return;
        }

        // Additional processing if it's an array element: varName[]
        if (isArray) {
            Obj arrayObj = designator.obj;
            Struct arrayIndexExpressionType = ((ArrayIndex) designator.getDesignatorArrayIndex()).getExpr().struct;

            // Check if the variable is an array name and if the index is of type int
            if (arrayObj.getType().getKind() != Struct.Array) {
                report_error("Variable " + varName + " must be an array name", designator);
                designator.obj = Tab.noObj;
            } else if (!arrayIndexExpressionType.equals(Tab.intType)) {
                report_error("Array index expression must be of type int", designator);
                designator.obj = Tab.noObj;
            } else {
                Struct elemType = arrayObj.getType().getElemType();
                designator.obj = new Obj(Obj.Elem, varName, elemType);
            }
        }

        // Detect the usage of Designator (variable name, array element or function) in expressions
        report_info("Access to variable or array element found", designator);
    }

    @Override
    public void visit(ArrayIndexPlaceholder placeholder) {
        // Preserve the object node address of the array itself (besides the element's in Designator.obj)
        Designator designator = (Designator) placeholder.getParent().getParent();

        Obj arrayObj;
        do {
            arrayObj = Tab.find(designator.getName());
            placeholder.obj = arrayObj;
        } while (arrayObj.getType().getKind() != Struct.Array);
    }

    /* ---------> 6. Productions related to Expr, Term, Factor, Const <--------- */

    /**
     * Set the type of the expression.
     * - It corresponds to the int type, as addition is performed.
     * - Check if the types of operands are also int.
     */
    @Override
    public void visit(ExprAddOp addExpr) {
        Struct exprType = addExpr.getExpr().struct;
        Struct termType = addExpr.getTerm().struct;

        // Check if the types of addition elements are 'int'
        if (termType.equals(exprType) && termType == Tab.intType) {
            addExpr.struct = termType;
        } else {
            report_error("Incompatible types in addition expression", addExpr);
            addExpr.struct = Tab.noType;
        }
    }

    @Override
    public void visit(ExprTermPositive termExpr) {
        termExpr.struct = termExpr.getTerm().struct;
    }

    @Override
    public void visit(ExprTermNegative termExpr) {
        // Check if 'term' is of type int (according to the specification)
        if (!((termExpr.struct = termExpr.getTerm().struct) == Tab.intType)) {
            report_error("In an expression of the form -X, type X must be of type int", termExpr);
            termExpr.struct = Tab.noType;
        }
    }

    @Override
    public void visit(TermMulOp term) {
        Struct termType = term.getTerm().struct;
        Struct factorType = term.getFactor().struct;

        // Check if the types of multiplication elements are 'int'
        if (termType.equals(factorType) && factorType == Tab.intType) {
            term.struct = factorType;
        } else {
            report_error("Incompatible types in multiplication expression", term);
            term.struct = Tab.noType;
        }
    }

    @Override
    public void visit(TermSingleFactor term) {
        term.struct = term.getFactor().struct;
    }

    @Override
    public void visit(FactorVar factor) {
        // Designator can be: variable name, function name, class field, or array element
        factor.struct = factor.getDesignator().obj.getType();
    }

    @Override
    public void visit(FactorConst factor) {
        // Constant can be of type: int, char, or bool
        factor.struct = factor.getConst().struct;
    }

    @Override
    public void visit(FactorNewArray factorNewArray) {
        // Check if the array size is of type int (according to the specification)
        if (factorNewArray.getExpr().struct != Tab.intType) {
            report_error("Array size must be of type int", factorNewArray);
            factorNewArray.struct = Tab.noType;
        } else {
            factorNewArray.struct = CommonUtils.getArrayType(factorNewArray.getType().struct);
        }
    }

    @Override
    public void visit(FactorExpression factorBracketExpression) {
        factorBracketExpression.struct = factorBracketExpression.getExpr().struct;
    }

    @Override
    public void visit(ConstInt cnst) {
        cnst.struct = Tab.intType;
    }

    @Override
    public void visit(ConstChar cnst) {
        cnst.struct = Tab.charType;
    }

    @Override
    public void visit(ConstBool cnst) {
        cnst.struct = CommonUtils.boolType;
    }

    /* Utility methods */
    public int getNumberOfVariables() {
        return numberOfVariables;
    }

    public boolean semanticAnalysisPassed() {
        return !errorDetected;
    }
}
