package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;

public class VirtualRegister extends IRRegister {
    public String vrName = null;
    public PhysicalRegister allocPhysicalReg = null;
    public IRMem spillOut = null;

    public VirtualRegister(String vrName){
        this.vrName = vrName;
    }

    public VirtualRegister(String vrName, PhysicalRegister ps){
        this.vrName = vrName;
        this.allocPhysicalReg = ps;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
