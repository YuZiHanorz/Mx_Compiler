package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.node.IRFunc;

public class StackSlot extends IRMem {
    public String name;
    public IRFunc parentFunc = null;

    public StackSlot(String name){
        this.name = name;
    }

    public StackSlot(String name, IRFunc parentFunc){
        this.name = name;
        this.parentFunc = parentFunc;
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }
}
