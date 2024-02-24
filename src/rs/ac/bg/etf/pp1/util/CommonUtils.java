package rs.ac.bg.etf.pp1.util;

import rs.ac.bg.etf.pp1.ast.Const;
import rs.ac.bg.etf.pp1.ast.ConstBool;
import rs.ac.bg.etf.pp1.ast.ConstChar;
import rs.ac.bg.etf.pp1.ast.ConstInt;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CommonUtils {
    /* Class fields */
    public static final Struct boolType = new Struct(Struct.Bool);

    private static int printBoolFunctionAddress;
    private static int findAnyFunctionAddress;
    private static int findAllFunctionAddress;

    private static int arrayLengthCheckFunctionAddress;
    private static int arrayMembersAccessCheckFunctionAddress;
    private static int arrayIndexCheckFunctionAddress;

    /* Getters and setters */
    public static int getPrintBoolFunctionAddress() {
        return printBoolFunctionAddress;
    }

    public static int getFindAnyFunctionAddress() {
        return findAnyFunctionAddress;
    }

    public static int getFindAllFunctionAddress() {
        return findAllFunctionAddress;
    }

    public static int getArrayLengthCheckFunctionAddress() {
        return arrayLengthCheckFunctionAddress;
    }

    public static int getArrayMembersAccessCheckFunctionAddress() {
        return arrayMembersAccessCheckFunctionAddress;
    }

    public static int getArrayIndexCheckFunctionAddress() {
        return arrayIndexCheckFunctionAddress;
    }

    /* Methods */
    public static void initSymbolTable() {
        Tab.init();
        Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
    }

    /**
     * Initializes code generation for:
     * - PRINT method for boolean values,
     * - calling 'findAny',
     * - functions for array parameter checks.
     */
    public static void initCodeGenerator() {
        // Add print instruction version for boolean type
        createPrintBoolMethod();

        // Add 'findAny' function
        createFindAnyMethod();
        createFindAllFunction();

        // Create array 'guardian' functions
        createArrayLengthCheckFunction();
        createArrayMembersAccessCheckFunction();
        createArrayIndexCheckFunction();
    }

    private static void createPrintBoolMethod() {
        // Address of the first instruction (important later when calling this method)
        printBoolFunctionAddress = Code.pc;

        // This function has 2 arguments ('val' and 'width') and 0 local parameters
        generateMethodInitialCode(2, 0);

        // Load the first argument 'val'
        // Insert instruction to push 'val' onto stack and the value 1 (for comparison)
        Code.put(Code.load_n);
        Code.loadConst(1);

        // Jump happens if 'val' is 1 (true)
        Code.putFalseJump(1, 0);
        int addressIfTrue = Code.pc - 2; // Address field to be modified

        // Otherwise, 'false' is printed, and there is an unconditional jump to avoid printing 'true'
        printStringLiteral("false");
        Code.putJump(0);
        int methodExitAddress = Code.pc - 2;

        // Set the address to the instruction below for printing 'true' if 1 (true) on the stack
        Code.fixup(addressIfTrue);
        printStringLiteral("true");

        // Set the address to the instruction below for exiting the method
        Code.fixup(methodExitAddress);
        generateMethodFinalCode();
    }

    private static void printStringLiteral(String value) {
        for (int i = 0; i < value.length(); i++) {
            // Create a character constant and load it onto the stack as 'val'
            Obj cnstObj = addConstToSymTable("$", Tab.charType, value.charAt(i));
            Code.load(cnstObj);

            // Load 'width' for the 'printBool' method
            if (i != 0) Code.loadConst(1);
            else Code.put(Code.load_1);

            // Generate 'print' instruction's code
            Code.put(Code.bprint);
        }
    }

    private static void createFindAnyMethod() {
        // Set the address for the findAny function
        findAnyFunctionAddress = Code.pc;

        // Generate the initial code for the method
        generateMethodInitialCode(2, 1);

        // Load the array address (PARAMETER 1)
        Code.put(Code.load_n);

        // Determine the length of the array for search boundary (PARAMETER 2)
        Code.put(Code.arraylength);

        /* LOOP_START */
        int loopStartAddress = Code.pc;

        // Put value 1 on the stack to decrement the index in each iteration
        Code.loadConst(1);

        // Decrease the index on the stack
        Code.put(Code.sub);

        // Store the index value into local variable at index '2'
        Code.put(Code.store_2);

        // Loop condition check
        Code.put(Code.load_2);
        Code.loadConst(0);

        // Compare the value on top of the stack (0) and the index
        // If < 0, program jumps to the address for failure search return
        Code.putFalseJump(5, 0);
        int loopExitFailAddress = Code.pc - 2; // Address field to be modified

        // Load the array element
        Code.put(Code.load_n);
        Code.put(Code.load_2);
        Code.put(Code.aload);

        // Load the value being searched for
        Code.put(Code.load_1);

        // Loop exits when a value equal to the searched one is found in the array
        Code.putFalseJump(1, 0);
        int loopExitSuccessAddress = Code.pc - 2;

        // Jump to the next iteration
        Code.put(Code.load_2);
        Code.putJump(loopStartAddress);

        /* LOOP_END */

        // Set return value to 0 if element is not found
        Code.fixup(loopExitFailAddress);
        Code.loadConst(0);
        Code.putJump(0);
        int methodExitAddress = Code.pc - 2; // Address field to be modified

        // Set return value to 1 if element is found
        Code.fixup(loopExitSuccessAddress);
        Code.loadConst(1);

        // Generate the final code for the method
        Code.fixup(methodExitAddress);
        generateMethodFinalCode();
    }

    public static void createFindAllFunction() {
        findAllFunctionAddress = Code.pc;
        generateMethodInitialCode(2, 2);

        // Load the array address (PARAMETER 1)
        Code.put(Code.load_n);

        // Determine the length of the array for search boundary (PARAMETER
        Code.put(Code.arraylength);

        /* LOOP_START */
        int loopStartAddress = Code.pc;

        // Put value 1 on the stack to decrement the index in each iteration
        Code.loadConst(1);

        // Decrease the index on the stack
        Code.put(Code.sub);

        // Store the index value into local variable at index '2'
        Code.put(Code.store_2);

        // Loop condition check
        Code.put(Code.load_2);
        Code.loadConst(0);

        // Compare the value on top of the stack (0) and the index
        // If < 0, program jumps to the address for failure search return
        Code.putFalseJump(5, 0);
        int loopExitFailAddress = Code.pc - 2; // Address field to be modified

        // Load the array element
        Code.put(Code.load_n);
        Code.put(Code.load_2);
        Code.put(Code.aload);

        // Load the value being searched for
        Code.put(Code.load_1);

        // Loop exits when a value equal to the searched one is found in the array
        Code.putFalseJump(1, 0);
        int loopExitSuccessAddress = Code.pc - 2;

        // Jump to the next iteration
        Code.put(Code.load_2);
        Code.putJump(loopStartAddress);

        /* LOOP_END */

        // Set return value to 0 if element is not found
        Code.fixup(loopExitFailAddress);
        Code.put(Code.load_3);
        Code.putJump(0);
        int methodExitAddress = Code.pc - 2; // Address field to be modified

        // Increment the counter of found elements
        Code.fixup(loopExitSuccessAddress);

        Code.put(Code.load_3);
        Code.loadConst(1);
        Code.put(Code.add);
        Code.put(Code.store_3);

        // Jump to the next iteration
        Code.put(Code.load_2);
        Code.putJump(loopStartAddress);

        Code.fixup(methodExitAddress);

        generateMethodFinalCode();
    }

    private static void createArrayLengthCheckFunction() {
        // Store the address of the function and generate the initial method code
        arrayLengthCheckFunctionAddress = Code.pc;
        generateMethodInitialCode(1, 0);

        // Load the array length onto the stack
        Code.put(Code.load_n);

        // Load 0 onto the stack (to compare with the array length)
        Code.loadConst(0);

        // If 'array length >= 0', jump forward to an unknown address
        Code.putFalseJump(2, 0);
        int validArrayLengthAddress = Code.pc - 2; // address field

        // In case of error, print message and halt (trap)
        printStringLiteral("\nArray length cannot be a negative number!");
        Code.put(Code.trap);
        Code.put(0);

        // Modify the jump address field
        Code.fixup(validArrayLengthAddress);

        // If no error, exit the function
        generateMethodFinalCode();
    }

    /**
     * Creates a function to check array members access.
     */
    private static void createArrayMembersAccessCheckFunction() {
        // Set the address for the array members access check function
        arrayMembersAccessCheckFunctionAddress = Code.pc;

        // Generate initial method code
        generateMethodInitialCode(1, 0);

        // Load the value at position 0 (array address) onto stack
        Code.put(Code.load_n);

        // Check if array address is zero (array not initialized), and if so, jump to error message
        Code.loadConst(0);
        Code.putFalseJump(0, 0);
        int arrayNotInitializedAddress = Code.pc - 2;

        // Print error message and trap in case of array access before initialization
        printStringLiteral("\nAccessing array elements before array initialization!");
        Code.put(Code.trap);
        Code.put(2);

        // Modify the jump address field for printing the message and 'trap'
        Code.fixup(arrayNotInitializedAddress);

        // Generate final method code and exit the function
        generateMethodFinalCode();
    }

    /**
     * Creates a function to check for array index boundaries
     */
    private static void createArrayIndexCheckFunction() {
        // Save the current program counter as the address of the array index check function
        arrayIndexCheckFunctionAddress = Code.pc;
        // Generate the initial code for the function
        generateMethodInitialCode(2, 0);

        // Load the array length and index of the array (already on the stack)
        Code.put(Code.load_n);
        Code.put(Code.arraylength); // Array length
        Code.put(Code.load_1); // Index of the array

        // If 'array length <= index', jump to error
        Code.putFalseJump(4, 0);
        int indexOverFlowAddress = Code.pc - 2;

        // Load the index and check if it's less than 0 (jump to error)
        Code.put(Code.load_1);
        Code.loadConst(0);
        Code.putFalseJump(5, 0);
        int indexUnderflowAddress = Code.pc - 2;

        // Generate the final code for the function
        generateMethodFinalCode();

        // Fixup the jump addresses for index overflow and underflow errors
        Code.fixup(indexOverFlowAddress);
        Code.fixup(indexUnderflowAddress);

        // Print error message in case of index out of bounds
        printStringLiteral("\nArray index is out of bounds!");
        Code.put(Code.trap);
        Code.put(1);
    }

    /* Utility functions */

    /**
     * Returns the integer equivalent of the constant value.
     *
     * @param cnst The constant object.
     * @return Returns the integer equivalent of the constant value.
     */
    public static int getConstValue(Const cnst) {
        int constValue = -1;

        if (cnst.getClass() == ConstInt.class) {
            constValue = ((ConstInt) cnst).getI1();
        } else if (cnst.getClass() == ConstChar.class) {
            constValue = ((ConstChar) cnst).getC1();
        } else if (cnst.getClass() == ConstBool.class) {
            constValue = ((ConstBool) cnst).getB1() ? 1 : 0;
        }
        return constValue;
    }

    /**
     * Adds a constant to the symbol table.
     *
     * @param name  Name of the constant.
     * @param type  Type of the constant.
     * @param value Value of the constant.
     * @return Returns the object node of the constant in the symbol table.
     */
    public static Obj addConstToSymTable(String name, Struct type, int value) {
        // Insert the constant into the symbol table
        Obj constObj = Tab.insert(Obj.Con, name, type);

        // Set the level of the constant to 0 (global data)
        constObj.setLevel(0);

        // Set the address field of the constant object to the provided value
        constObj.setAdr(value);

        // Return the object node of the constant in the symbol table
        return constObj;
    }

    /**
     * Returns the Struct object for an array of elements of type 'elemType'.
     *
     * @param elemType Struct object for the type of array elements.
     * @return Returns the Struct object of the array.
     */
    public static Struct getArrayType(Struct elemType) {
        return new Struct(Struct.Array, elemType);
    }

    public static void generateMethodInitialCode(int formalParamCount, int localVarCount) {
        Code.put(Code.enter);
        Code.put(formalParamCount);
        Code.put(formalParamCount + localVarCount);
    }

    public static void generateMethodFinalCode() {
        // Destroy the method activation record on the stack
        Code.put(Code.exit);

        // Return to the caller
        Code.put(Code.return_);
    }

    public static void generateMethodCall(int methodAddress) {
        // Generate bytecode for calling a method whose address is given.
        int offset = methodAddress - Code.pc;
        Code.put(Code.call);
        Code.put2(offset);
    }
}
