package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.mj.runtime.*;
import rs.ac.bg.etf.pp1.CounterVisitor.*;
import rs.ac.bg.etf.pp1.util.CommonUtils;

public class CodeGenerator extends VisitorAdaptor {
    /* Class fields */
    private static final int DEFAULT_PRINT_WIDTH = 5;
    private int mainPC; // Address of the first instruction of the main method (program) that the VM will execute

    /* ---------> 0. Productions related to the program <--------- */

    @Override
    public void visit(ProgName progName) {
        CommonUtils.initCodeGenerator();
    }

    /* ---------> 1. Productions related to constant declarations <--------- */

    /* ---------> 2. Productions related to variable declarations <--------- */

    /* ---------> 3. Productions related to method declarations <--------- */

    /**
     * Consider code generation actions upon entering a method declaration.
     * <p>
     * Step by step:
     * - set the value of the address of the first method instruction (methodObj.setAdr),
     * - set mainPC if it is the 'main' method,
     * - count local variables of the method by traversing the tree rooted at the 'MethodDecl' node,
     * - set the code of the Code.enter instruction (creates an activation record),
     * - set the number of formal parameters on the stack,
     * - set the sum of the number of parameters and the number of local variables on the stack.
     */
    @Override
    public void visit(MethodTypeName methodTypeName) {
        Obj methodObj = methodTypeName.obj;

        // Set the method address field to the address of the first instruction in its body (Code.pc)
        methodObj.setAdr(Code.pc);

        // If it's the 'main' method, set the 'mainPC' value as well
        if ("main".equalsIgnoreCase(methodTypeName.getName())) {
            this.mainPC = Code.pc;
        }

        // Get the 'MethodDecl' node parent of the given node
        SyntaxNode methodDeclParentNode = methodTypeName.getParent();

        // Count the declarations of local variables for this method
        LocalVarCounter localVarCounter = new LocalVarCounter();
        methodDeclParentNode.traverseTopDown(localVarCounter);

        // Generate the 'enter' instruction for entering the method
        CommonUtils.generateMethodInitialCode(0, localVarCounter.getCount());
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // Generate instructions that are executed at the end of the method
        CommonUtils.generateMethodFinalCode();
    }

    /* ---------> 4. Language statements: Print, Return, Read <--------- */

    /**
     * PRINT expects 'val' (int, char) and 'width' to already be present on the stack.
     * <p>
     * Looking at the production, 'Expr' pushed 'val' onto the stack during processing,
     * and here we only need to generate an instruction to push 'width'.
     * <p>
     * Finally, we should generate bytecode for the 'print' instruction itself.
     */
    @Override
    public void visit(StatementPrint printStmt) {
        // Check if 'width' is specified for this instruction
        int widthValue = DEFAULT_PRINT_WIDTH;
        if (printStmt.getPrintParam().getClass() == PrintWidth.class) {
            widthValue = ((PrintWidth) printStmt.getPrintParam()).getWidth();
        }
        Code.loadConst(widthValue);

        // Depending on the type of 'var' value, generate bytecode for the corresponding 'print' instruction
        if (printStmt.getExpr().struct == Tab.intType) {
            Code.put(Code.print);
        } else if (printStmt.getExpr().struct == Tab.charType) {
            Code.put(Code.bprint);
        } else { //boolType
            CommonUtils.generateMethodCall(CommonUtils.getPrintBoolFunctionAddress());
        }
    }

    /**
     * Depending on whether the input value is 'char' or not, 'bread' or 'read' is generated.
     * Then its argument is placed on the stack.
     */
    @Override
    public void visit(StatementRead statementRead) {
        Struct argType = statementRead.getDesignator().obj.getType();
        Code.put(argType == Tab.charType ? Code.bread : Code.read);

        Code.store(statementRead.getDesignator().obj);
    }

    /* ---------> 5. Productions related to Designator and DesignatorStatement <--------- */

    @Override
    public void visit(DesignatorInc designatorInc) {
        // Here, the value of the sum of the constant 1 and the value of the variable is actually assigned
        // So we put the value of the variable on the stack, then the value 1, then perform the 'add' operation
        Obj designatorObj = designatorInc.getDesignator().obj;

        switch (designatorObj.getKind()) {
            case Obj.Var:
                // Put the variable on top of the stack (generates instructions: load_n or getstatic n)
                Code.load(designatorObj);
                break;
            case Obj.Elem:
                // Due to previously visiting the 'DesignatorArrayIndex' node, we have: array address and element index on the stack
                // To get elem := elem + 1, we duplicate the values and put the array element on the stack
                Code.put(Code.dup2);
                Code.load(designatorObj);
                break;
        }

        Code.loadConst(1);
        Code.put(Code.add);

        // After this, 'add' will generate and put the incremented result on the stack
        // We generate the instruction that takes this value and stores it into the 'designator' destination
        /*
         * The call to store() below generates:
         * - code for store_n or putstatic for variables,
         * - astore for arrays (expects array address and index on the stack, which were placed earlier,
         * - in the 'switch' above, and after them the computed value, i.e., the add result)
         */
        Code.store(designatorObj);
    }

    @Override
    public void visit(DesignatorDec designatorDec) {
        Obj designatorObj = designatorDec.getDesignator().obj;

        switch (designatorObj.getKind()) {
            case Obj.Var:
                Code.load(designatorObj);
                break;
            case Obj.Elem:
                Code.put(Code.dup2);
                Code.load(designatorObj);
                break;
        }

        Code.loadConst(1);
        Code.put(Code.sub);

        // After this, 'sub' will generate and put the decremented result on the stack
        // We need to put the instruction that takes this value and stores it into the 'designator' destination
        Code.store(designatorObj);
    }

    /**
     * Production: (Assignment) Designator:dest EQUAL AssignmentValue SEMI
     * The source is traversed in 'AssignmentValue', so the value is placed on the stack.
     * <p>
     * If it's an array, do additional marked processing.
     * <p>
     * It is necessary to generate the 'store' instruction that writes the value from the source into the destination.
     * - If 'dest' is a global variable: 'putstatic' instruction is generated.
     * - If 'dest' is a local variable: 'store' instruction is generated.
     */
    @Override
    public void visit(Assignment assignment) {
        // Generate an instruction that stores the value into the 'destination'
        Code.store(assignment.getDesignator().obj);
    }

    /**
     * Since both 'Designator' and 'Expr' are visited, the 'array address' and 'expression' are on the stack.
     * Only the code for the method call to 'findAny' needs to be generated.
     */
    @Override
    public void visit(AssignmentValueFindAny assignmentValue) {
        CommonUtils.generateMethodCall(CommonUtils.getFindAnyFunctionAddress());
    }

    @Override
    public void visit(AssignmentValueFindAll assignmentValue) {
        CommonUtils.generateMethodCall(CommonUtils.getFindAllFunctionAddress());
    }

    /**
     * Generate the value of the 'designator' on the stack if it's part of the assigned expression.
     * - In the first case, generate the array address (we must do that here).
     * - In the second case, generate the variable value (not necessarily here, it can also be in 'FactorVar').
     * - - If it's a 'global variable': generate code for 'getstatic' instruction.
     * - - If it's a 'local variable': generate code for 'load' instruction.
     */
    @Override
    public void visit(Designator designator) {
        SyntaxNode parent = designator.getParent();

        if (parent.getClass() == AssignmentValueFindAny.class || parent.getClass() == AssignmentValueFindAll.class || parent.getClass() == FactorVar.class) {
            Code.load(designator.obj);
        }
    }

    /**
     * Check the access to array elements and the array index value.
     * - Access checks are performed with "guards" calls.
     * - If the condition for accessing the array or index is not met, "guards" may cause an error.
     */
    @Override
    public void visit(ArrayIndex arrayIndex) {
        // Copy the array address and index before checking array element access
        Code.put(Code.dup2);

        // Leave only the array address on the top of the stack
        Code.put(Code.pop);

        // Generate the method call for checking array element access (takes the value off the top of the stack)
        CommonUtils.generateMethodCall(CommonUtils.getArrayMembersAccessCheckFunctionAddress());

        // Copy the address and index again, then check the array index value
        Code.put(Code.dup2);
        CommonUtils.generateMethodCall(CommonUtils.getArrayIndexCheckFunctionAddress());
    }

    @Override
    public void visit(ArrayIndexPlaceholder placeholder) {
        // It must place the array address before encountering 'Expr:arrayIndex'
        Code.load(placeholder.obj);
    }

    /* ---------> 6. Productions related to Expr, Term, Factor, Const <--------- */

    @Override
    public void visit(ExprAddOp addExpr) {
        // Nodes of operands are already visited, and values pushed onto the stack
        if (addExpr.getAddOperator().getClass() == PlusOp.class) {
            Code.put(Code.add);
        } else {
            Code.put(Code.sub);
        }
    }

    @Override
    public void visit(ExprTermNegative exprTermNegative) {
        // The value has already been placed on the stack by visiting 'Term'; it needs to be negated
        Code.put(Code.neg);
    }

    @Override
    public void visit(TermMulOp mulTerm) {
        MulOperator mulOperator = mulTerm.getMulOperator();

        // Place the operation code onto the stack
        if (mulOperator.getClass() == MulOp.class) {
            Code.put(Code.mul);
        } else if (mulOperator.getClass() == DivideOp.class) {
            Code.put(Code.div);
        } else Code.put(Code.rem);
    }

    @Override
    public void visit(FactorNewArray factorNewArray) {
        // The array length is already on the stack due to traversal: FactorNewArray -> Expr
        // Duplicate the array length value and generate a method call to check the length
        Code.put(Code.dup);
        CommonUtils.generateMethodCall(CommonUtils.getArrayLengthCheckFunctionAddress()); // Pops an element off the top of the stack

        // Generate code instruction for array creation
        Code.put(Code.newarray);

        // Generate 0 for char type (array of characters) or 1 (for int, bool, class type)
        Code.put(factorNewArray.struct.getElemType() == Tab.charType ? 0 : 1);
    }

    @Override
    public void visit(FactorConst factor) {
        Const cnst = factor.getConst();
        int constValue = CommonUtils.getConstValue(cnst);

        // Create a global unnamed constant in the symbol table
        Obj cnstObj = CommonUtils.addConstToSymTable("$", cnst.struct, constValue);

        // Since 'FactorConst' is part of 'Expr', generate an instruction (const or const_x) to push the constant onto the stack
        Code.load(cnstObj);
    }

    /* Utility methods */
    public int getMainPC() {
        return mainPC;
    }
}
