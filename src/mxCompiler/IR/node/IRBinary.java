package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.*;
import mxCompiler.Utility.RegCollection;

import java.util.HashMap;
import java.util.LinkedList;

public class IRBinary extends IRInst{
    public enum Bop {
        ADD, SUB, MUL, DIV, MOD, SAL, SAR, AND, OR, XOR
    }

    public Bop bop;
    public IRStorePos dest;
    public Operand rt;

    public IRBinary(BasicBlock parentBB, Bop bop, IRStorePos dest, Operand src){
        super(parentBB);
        this.bop = bop;
        this.dest = dest;
        this.rt = src;
    }

    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();

        if (dest instanceof IRMem)
            list.addAll(((IRMem) dest).getUsedRegs());
        else if (dest instanceof IRRegister)
            list.add((IRRegister)dest);

        if (rt instanceof IRMem)
            list.addAll(((IRMem) rt).getUsedRegs());
        else if (rt instanceof IRRegister)
            list.add((IRRegister)rt);

        if (bop == Bop.MUL) {
            if (!list.contains(RegCollection.vrax))
                list.add(RegCollection.vrax);
        }
        else if (bop == Bop.DIV){
            if (!list.contains(RegCollection.vrax))
                list.add(RegCollection.vrax);
            if (!list.contains(RegCollection.vrdx))
                list.add(RegCollection.vrdx);
        }
        else if (bop == Bop.MOD){
            if (!list.contains(RegCollection.vrax))
                list.add(RegCollection.vrax);
            if (!list.contains(RegCollection.vrdx))
                list.add(RegCollection.vrdx);
        }

        return list;
    }
    @Override
    public LinkedList<IRRegister> getDefRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();
        if (dest instanceof IRRegister)
            list.add((IRRegister) dest);
        if (bop == Bop.MUL){
            if (!list.contains(RegCollection.vrax))
                list.add(RegCollection.vrax);
        }
        else if (bop == Bop.DIV || bop == Bop.MOD){
            if (!list.contains(RegCollection.vrax))
                list.add(RegCollection.vrax);
            if (!list.contains(RegCollection.vrdx))
                list.add(RegCollection.vrdx);
        }
        return list;
    }

    @Override
    public LinkedList<StackSlot> getStackSlots(){
        return getDefaultStackSlots(dest, rt);
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        if (dest instanceof IRMem){
            dest = ((IRMem) dest).copy();
            ((IRMem) dest).renameUsedReg(renameMap);
        }
        else if (dest instanceof IRRegister && renameMap.containsKey(dest))
            dest = renameMap.get(dest);

        if (rt instanceof IRMem){
            rt = ((IRMem) rt).copy();
            ((IRMem) rt).renameUsedReg(renameMap);
        }
        else if (rt instanceof IRRegister && renameMap.containsKey(rt))
            rt = renameMap.get(rt);
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
