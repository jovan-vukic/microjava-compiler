package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.VarDecl;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class CounterVisitor extends VisitorAdaptor {
    // Class fields
    protected int count;

    // Static class that counts local variable declarations
    public static class LocalVarCounter extends CounterVisitor {
        @Override
        public void visit(VarDecl VarDecl) {
            // Extends 'CounterVisitor' thus inheriting the 'count' field
            count++;
        }
    }

    // Getters and setters
    public int getCount() {
        return count;
    }
}
