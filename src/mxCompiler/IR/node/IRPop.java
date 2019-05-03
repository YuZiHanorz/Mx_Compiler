package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.IRMem;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.IRStorePos;
import mxCompiler.IR.operand.StackSlot;

import java.util.HashMap;
import java.util.LinkedList;

public class IRPop extends IRInst{
    public IRStorePos dest;

    public IRPop(BasicBlock parentBB, IRStorePos pos){
        super(parentBB);
        this.dest = pos;
    }


    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();

        if (dest instanceof IRMem)
            list.addAll(((IRMem) dest).getUsedRegs());
        //else if (dest instanceof IRRegister)
        //list.add((IRRegister) dest);

        return list;
    }

    @Override
    public LinkedList<IRRegister> getDefRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();
        if (dest instanceof IRRegister)
            list.add((IRRegister)dest);
        return list;
    }

    @Override
    public LinkedList<StackSlot> getStackSlots(){
        return getDefaultStackSlots(dest);
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        if (dest instanceof IRMem){
            dest = ((IRMem) dest).copy();
            ((IRMem) dest).renameUsedReg(renameMap);
        }
        //else if (dest instanceof IRRegister && renameMap.containsKey(dest))
        //dest = renameMap.get(dest);

    }

    @Override
    public void renameDefReg(HashMap<IRRegister, IRRegister> renameMap){
        if (dest instanceof IRRegister && renameMap.containsKey(dest))
            dest = renameMap.get(dest);
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }


}
