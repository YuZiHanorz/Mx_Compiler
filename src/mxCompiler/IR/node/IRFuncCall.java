package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.*;
import mxCompiler.Utility.RegCollection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class IRFuncCall extends IRInst {
    public IRStorePos dest;
    public IRFunc func;
    public LinkedList<Operand> argList;

    public IRFuncCall(BasicBlock parentBB, IRStorePos dest, IRFunc func, LinkedList<Operand> args){
        super(parentBB);
        this.dest = dest;
        this.func = func;
        this.argList = new LinkedList<>(args);
        IRFunc caller = parentBB.parentFunc;
        caller.calleeFuncs.add(func);
        if (func.funcName.equals("print")|| func.funcName.equals("println"))
            caller.hasPrint = true;
    }

    public IRFuncCall(BasicBlock parentBB, IRStorePos dest, IRFunc func, Operand... args){
        super(parentBB);
        this.dest = dest;
        this.func = func;
        this.argList = new LinkedList<>(Arrays.asList(args));
        IRFunc caller = parentBB.parentFunc;
        caller.calleeFuncs.add(func);
        if (func.funcName.equals("print")|| func.funcName.equals("println"))
            caller.hasPrint = true;
    }


    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        return new LinkedList<>(RegCollection.vArgRegList.subList(0, Integer.min(6, argList.size())));
    }

    @Override
    public LinkedList<IRRegister> getDefRegs(){
        LinkedList<IRRegister> regs = new LinkedList<>(RegCollection.vCallerSaveRegList);
        return regs;
    }

    @Override
    public LinkedList<StackSlot> getStackSlots(){
        LinkedList<StackSlot> list = new LinkedList<>(getDefaultStackSlots(dest));
        for (Operand o : argList){
            if (o instanceof StackSlot)
                list.add((StackSlot)o);
        }
        return list;
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        //do nothing
    }

    @Override
    public void renameDefReg(HashMap<IRRegister, IRRegister> renameMap){
        if (dest instanceof IRRegister && renameMap.containsKey(dest))
            dest = renameMap.get(dest);
    }

    public LinkedList<IRRegister> getCallUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();
        for (Operand o : argList){
            if (o instanceof IRMem)
                list.addAll(((IRMem) o).getUsedRegs());
            else if (o instanceof IRRegister){
                list.add((IRRegister)o);
            }
        }
        return list;
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }


}
