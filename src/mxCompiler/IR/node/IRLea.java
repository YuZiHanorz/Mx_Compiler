package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.IRMem;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.StackSlot;

import java.util.HashMap;
import java.util.LinkedList;

public class IRLea extends IRInst{
    public IRRegister destReg;
    public IRMem srcMem;

    public IRLea(BasicBlock parentBB, IRRegister dest, IRMem m){
        super(parentBB);
        this.destReg = dest;
        this.srcMem = m;
    }

    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>(srcMem.getUsedRegs());
        list.add(destReg);
        return list;
    }

    @Override
    public LinkedList<IRRegister> getDefRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();
        list.add(destReg);
        return list;
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        srcMem = srcMem.copy();
        srcMem.renameUsedReg(renameMap);
    }

    @Override
    public void renameDefReg(HashMap<IRRegister, IRRegister> renameMap){
        if (renameMap.containsKey(destReg))
            destReg= renameMap.get(destReg);
    }

    @Override
    public LinkedList<StackSlot> getStackSlots(){
        return getDefaultStackSlots(srcMem);
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }

}
