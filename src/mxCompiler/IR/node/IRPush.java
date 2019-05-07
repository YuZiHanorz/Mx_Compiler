package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.IRMem;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.Operand;
import mxCompiler.IR.operand.StackSlot;

import java.util.HashMap;
import java.util.LinkedList;

public class IRPush extends IRInst{
    public Operand src;

    public IRPush(BasicBlock parentBB, Operand src){
        super(parentBB);
        this.src = src;
    }

    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();

        if (src instanceof IRMem)
            list.addAll(((IRMem) src).getUsedRegs());
        //else if (src instanceof IRRegister)
          //  list.add((IRRegister) src);

        return list;
    }

    @Override
    public LinkedList<IRRegister> getDefRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();
        if (src instanceof IRRegister)
            list.add((IRRegister)src);
        return list;
    }

    @Override
    public LinkedList<StackSlot> getStackSlots(){
        return getDefaultStackSlots(src);
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        if (src instanceof IRMem){
            src = ((IRMem) src).copy();
            ((IRMem) src).renameUsedReg(renameMap);
        }
        else if (src instanceof IRRegister && renameMap.containsKey(src))
            src = renameMap.get(src);

    }

    @Override
    public void renameDefReg(HashMap<IRRegister, IRRegister> renameMap){
        if (src instanceof IRRegister && renameMap.containsKey(src))
            src = renameMap.get(src);
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }

}
