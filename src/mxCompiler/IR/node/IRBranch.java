package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.*;

import java.util.HashMap;
import java.util.LinkedList;

public class IRBranch extends IRInst{
    public enum Cop{
        E, NE, G, L, GE, LE
    }

    public Cop op;
    public Operand lt, rt;
    public BasicBlock thenBB;
    public BasicBlock elseBB;

    public IRBranch(BasicBlock parentBB, Cop op, Operand lt, Operand rt, BasicBlock thenBB, BasicBlock elseBB){
        super(parentBB);
        this.op = op;
        this.lt = lt;
        this.rt = rt;
        this.thenBB = thenBB;
        this.elseBB = elseBB;
    }

    public Cop reverseOp(){
        switch(op) {
            case E:
                return Cop.E;
            case NE:
                return Cop.NE;
            case G:
                return Cop.LE;
            case L:
                return Cop.GE;
            case GE:
                return Cop.L;
            case LE:
                return Cop.G;
            default:
                throw new Error("what branch Op is it?");
        }
    }

    private Cop swapOp(){
        switch(op) {
            case E:
                return Cop.NE;
            case NE:
                return Cop.E;
            case G:
                return Cop.LE;
            case L:
                return Cop.GE;
            case GE:
                return Cop.L;
            case LE:
                return Cop.G;
            default:
                throw new Error("what branch Op is it?");
        }
    }

    public void reverse(){
        op = reverseOp();
        Operand t = lt;
        lt = rt;
        rt = t;
    }

    public void swap(){
        op = swapOp();
        BasicBlock b = thenBB;
        thenBB = elseBB;
        elseBB = b;
    }

    public BasicBlock CalcDestBlock(){
        if (lt instanceof IntImm && rt instanceof IntImm){
            int vlt = ((IntImm) lt).value;
            int vrt = ((IntImm) rt).value;
            boolean flag;
            if (op == Cop.E)
                flag = vlt == vrt;
            else if (op == Cop.NE)
                flag = vlt != vrt;
            else if (op == Cop.G)
                flag = vlt > vrt;
            else if (op == Cop.L)
                flag = vlt < vrt;
            else if (op == Cop.GE)
                flag = vlt >= vrt;
            else if (op == Cop.LE)
                flag = vlt <= vrt;
            else throw new Error("what branch Op is it");
            if (flag)
                return thenBB;
            else return elseBB;
        }
        else throw new Error("cannot do cmp in IRBranch");
    }

    @Override
    public LinkedList<IRRegister> getUsedRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();

        if (lt instanceof IRMem)
            list.addAll(((IRMem) lt).getUsedRegs());
        else if (lt instanceof IRRegister)
            list.add((IRRegister) lt);

        if (rt instanceof IRMem)
            list.addAll(((IRMem) rt).getUsedRegs());
        else if (rt instanceof IRRegister)
            list.add((IRRegister) rt);

        return list;
    }

    @Override
    public LinkedList<IRRegister> getDefRegs(){
        LinkedList<IRRegister> list = new LinkedList<>();
        return list;
    }

    @Override
    public LinkedList<StackSlot> getStackSlots(){
        return getDefaultStackSlots(lt, rt);
    }

    @Override
    public void renameUsedReg(HashMap<IRRegister, IRRegister> renameMap){
        if (lt instanceof IRMem){
            lt = ((IRMem) lt).copy();
            ((IRMem) lt).renameUsedReg(renameMap);
        }
        else if (lt instanceof IRRegister && renameMap.containsKey(lt))
            lt = renameMap.get(lt);

        if (rt instanceof IRMem){
            rt = ((IRMem) rt).copy();
            ((IRMem) rt).renameUsedReg(renameMap);
        }
        else if (rt instanceof IRRegister && renameMap.containsKey(rt))
            rt = renameMap.get(rt);
    }

    @Override
    public void renameDefReg(HashMap<IRRegister, IRRegister> renameMap){
        //do nothing
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }
}
