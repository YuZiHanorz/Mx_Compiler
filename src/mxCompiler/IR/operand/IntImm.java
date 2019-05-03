package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;

public class IntImm extends IRConst{
    public int value;

    public IntImm(int v){
        this.value = v;
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }
}
