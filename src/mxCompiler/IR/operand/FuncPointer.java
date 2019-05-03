package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.node.IRFunc;

public class FuncPointer extends IRConst{
    public IRFunc func = null;

    public FuncPointer(IRFunc func){
        this.func = func;
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }
}
