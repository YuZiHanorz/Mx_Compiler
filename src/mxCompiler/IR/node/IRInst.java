package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.Operand;
import mxCompiler.IR.operand.StackSlot;

import java.util.HashMap;
import java.util.LinkedList;

abstract public class IRInst {
    public BasicBlock parentBB = null;
    public IRInst prevInst = null;
    public IRInst nxtInst = null;
    private boolean removed = false;

    public IRInst(){}

    public IRInst(BasicBlock b){
        this.parentBB = b;
    }

    public void prependInst(IRInst i){
        if (prevInst != null){
            this.prevInst.nxtInst = i;
            i.prevInst = this.prevInst;
            i.nxtInst = this;
            this.prevInst = i;
        }
        else {
            this.prevInst = i;
            i.nxtInst = this;
            this.parentBB.firstInst = i;
        }
    }

    public void appendInst(IRInst i){
        if (nxtInst != null){
            this.nxtInst.prevInst = i;
            i.nxtInst = this.nxtInst;
            i.prevInst = this;
            this.nxtInst = i;
        }
        else {
            this.nxtInst = i;
            i.prevInst = this;
            this.parentBB.lastInst = i;
        }
    }

    public void removeInst(){
        if (removed)
            throw new Error("cannot remove a removed Inst");
        else if (prevInst != null && nxtInst != null){
            prevInst.nxtInst = nxtInst;
            nxtInst.prevInst = prevInst;
        }
        else if (prevInst != null){
            prevInst.nxtInst = null;
            parentBB.lastInst = prevInst;
        }
        else if (nxtInst != null){
            nxtInst.prevInst = null;
            parentBB.firstInst = nxtInst;
        }
        else {
            parentBB.firstInst = parentBB.lastInst = null;
        }
        removed = true;
    }

    public void replaceInst(IRInst i){
        if (prevInst != null && nxtInst != null){
            prevInst.nxtInst = i;
            nxtInst.prevInst = i;
            i.prevInst = prevInst;
            i.nxtInst = nxtInst;
        }
        else if (prevInst != null){
            prevInst.nxtInst = i;
            i.prevInst = prevInst;
            i.nxtInst = null;
            parentBB.lastInst = i;
        }
        else if (nxtInst != null){
            nxtInst.prevInst = i;
            i.prevInst = null;
            i.nxtInst = nxtInst;
            parentBB.firstInst = i;
        }
        else {
            i.prevInst = i.nxtInst = null;
            parentBB.firstInst = parentBB.lastInst = i;
        }
    }
    public abstract LinkedList<IRRegister> getUsedRegs();
    public abstract LinkedList<IRRegister> getDefRegs();

    public abstract LinkedList<StackSlot> getStackSlots();
    public LinkedList<StackSlot> getDefaultStackSlots(Operand... operands) {
        LinkedList<StackSlot> list = new LinkedList<>();
        for(Operand o : operands)
            if(o instanceof StackSlot)
                list.add((StackSlot) o);
        return list;
    }

    public abstract void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap);
    public abstract void renameDefReg(HashMap<IRRegister, IRRegister> renameMap);

    abstract public void accept(IRVisitor visitor);





}
