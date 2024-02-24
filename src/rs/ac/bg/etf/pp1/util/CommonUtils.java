package rs.ac.bg.etf.pp1.util;

import rs.ac.bg.etf.pp1.ast.Const;
import rs.ac.bg.etf.pp1.ast.ConstBool;
import rs.ac.bg.etf.pp1.ast.ConstChar;
import rs.ac.bg.etf.pp1.ast.ConstInt;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CommonUtils {
    /* Class fields */
    public static final Struct boolType = new Struct(Struct.Bool);

    /* Methods */
    public static void initSymbolTable() {
        Tab.init();
        Tab.currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
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
}
