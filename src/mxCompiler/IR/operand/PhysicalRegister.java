package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;

public class PhysicalRegister extends IRRegister{
    public String name = null;

    public PhysicalRegister(){}

    public PhysicalRegister(String name){
        this.name = name;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

}
