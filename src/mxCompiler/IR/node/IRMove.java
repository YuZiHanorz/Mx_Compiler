package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.*;

import java.util.HashMap;
import java.util.LinkedList;

public class IRMove extends IRInst {
    public IRStorePos dest;
    public Operand src;

    public IRMove(BasicBlock parentBB, IRStorePos dest, Operand src){
        super(parentBB);
        if (dest == null || src == null)
            throw new Error("What fucking Move?");
        this.dest = dest;
        this.src = src;
    }

    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();

        if (dest instanceof IRMem)
            list.addAll(((IRMem) dest).getUsedRegs());
        //else if (dest instanceof IRRegister)
            //list.add((IRRegister) dest);

        if (src instanceof IRMem)
            list.addAll(((IRMem) src).getUsedRegs());
        else if (src instanceof IRRegister)
            list.add((IRRegister) src);

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
        return getDefaultStackSlots(dest, src);
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        if (dest instanceof IRMem){
            dest = ((IRMem) dest).copy();
            ((IRMem) dest).renameUsedReg(renameMap);
        }
        //else if (dest instanceof IRRegister && renameMap.containsKey(dest))
            //dest = renameMap.get(dest);

        if (src instanceof IRMem){
            src = ((IRMem) src).copy();
            ((IRMem) src).renameUsedReg(renameMap);
        }
        else if (src instanceof IRRegister && renameMap.containsKey(src))
            src = renameMap.get(src);
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
