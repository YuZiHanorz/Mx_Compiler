package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;

import java.util.HashMap;
import java.util.LinkedList;

public class IRMem extends IRStorePos {
    public IRRegister baseReg = null;
    public IRRegister indexReg = null;
    public int scale;
    public IRConst literal = null;

    public IRMem(){
        scale = 0;
    }

    public IRMem(IRRegister base, IRRegister index, int s, IRConst c){
        scale = 0;
        baseReg = base;
        indexReg = index;
        scale = s;
        literal = c;
    }

    public IRMem copy(){
        if (this instanceof StackSlot)
            return this;
        else return new IRMem(baseReg, indexReg, scale, literal);
    }

    public LinkedList<IRRegister> getUsedRegs() {
        LinkedList<IRRegister> list = new LinkedList<>();
        if (baseReg != null)
            list.add(baseReg);
        if (indexReg != null)
            list.add(indexReg);
        return list;
    }


    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap) {
        if(renameMap.containsKey(baseReg))
            baseReg = renameMap.get(baseReg);
        if(renameMap.containsKey(indexReg))
            indexReg = renameMap.get(indexReg);
    }


    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
