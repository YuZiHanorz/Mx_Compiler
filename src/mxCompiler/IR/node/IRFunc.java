package mxCompiler.IR.node;

import mxCompiler.Frontend.IRBuilder;
import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.PhysicalRegister;
import mxCompiler.IR.operand.VirtualRegister;
import mxCompiler.Symbol.VarSymbol;
import mxCompiler.Utility.RegCollection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class IRFunc {
    public String funcName = null;
    public boolean isCustom;
    public boolean isVoid;
    public boolean hasPrint = false;

    public BasicBlock firstBB = null;
    public BasicBlock lastBB = null;
    public LinkedList<BasicBlock> sonBB;
    public LinkedList<BasicBlock> reversePostOrder;
    public LinkedList<BasicBlock> reversePreOrder;

    public HashSet<VarSymbol> usedGlobalVars;
    public HashSet<PhysicalRegister> usedPhysicalRegs;

    public HashSet<IRFunc> calleeFuncs;
    public HashSet<VarSymbol> recursiveUsedGlobalVars;

    public LinkedList<VirtualRegister> paraVirtualRegs;

    public IRFunc(String funcName, boolean isCustom, boolean isVoid){
        this.funcName = funcName;
        this.isCustom = isCustom;
        this.isVoid = isVoid;
        this.sonBB = new LinkedList<>();
        this.reversePostOrder = new LinkedList<>();
        this.reversePreOrder = new LinkedList<>();
        this.usedGlobalVars = new HashSet<>();
        this.usedPhysicalRegs = new HashSet<>();
        this.calleeFuncs = new HashSet<>();
        this.recursiveUsedGlobalVars = new HashSet<>();
        this.paraVirtualRegs = new LinkedList<>();

        if (!isCustom && !funcName.equals("init")){
            for (PhysicalRegister reg : RegCollection.regList){
                if (reg.name.equals("rsp") || reg.name.equals("rbp"))
                    continue;
                this.usedPhysicalRegs.add(reg);
            }
        }
    }

    public void build(){
        for (BasicBlock b : sonBB){
            b.preds.clear();
            b.succ.clear();
        }

        for (BasicBlock b : sonBB){
            if (b.lastInst instanceof IRJump)
                b.succ.add(((IRJump) b.lastInst).destBB);
            else if (b.lastInst instanceof IRBranch){
                b.succ.add(((IRBranch) b.lastInst).thenBB);
                b.succ.add(((IRBranch) b.lastInst).elseBB);
            }
            for (BasicBlock s : b.succ)
                s.preds.add(b);
        }

        for (BasicBlock b : sonBB){
            if (b.lastInst instanceof IRBranch){
                if (((IRBranch) b.lastInst).thenBB.preds.size() < ((IRBranch) b.lastInst).elseBB.preds.size())
                    ((IRBranch) b.lastInst).swap();
            }
        }

        calcReversePostOrder();
        calcReversePreOrder();
        calcRecursiveUsedGlobalVars();
    }

    //make out func's using regs
    public void calcUsingPreg(){
        for (BasicBlock b : sonBB){
            for (IRInst i = b.firstInst; i != null; i = i.nxtInst) {
                if (i instanceof IRReturn)
                    continue;
                if (i instanceof IRFuncCall){
                    usedPhysicalRegs.addAll(RegCollection.callerSaveRegList);
                    continue;
                }
                if (i instanceof IRBinary){
                    IRBinary.Bop op = ((IRBinary) i).bop;
                    if (op != IRBinary.Bop.MUL && op != IRBinary.Bop.DIV && op != IRBinary.Bop.MOD){
                        usedPhysicalRegs.addAll(toPreg(i.getDefRegs()));
                        usedPhysicalRegs.addAll(toPreg(i.getUsedRegs()));
                        continue;
                    }
                    usedPhysicalRegs.add(RegCollection.rax);
                    usedPhysicalRegs.add(RegCollection.rdx);
                    if (((IRBinary) i).rt instanceof IRRegister)
                        usedPhysicalRegs.add((PhysicalRegister) ((IRBinary) i).rt);
                    continue;
                }
                usedPhysicalRegs.addAll(toPreg(i.getDefRegs()));
                usedPhysicalRegs.addAll(toPreg(i.getUsedRegs()));
            }
        }
    }

    private HashSet<BasicBlock> dfsVisitedBB = null;
    private HashSet<IRFunc> dfsVisitedFunc = null;

    private void dfsReversePostOrder(BasicBlock b) {
        if(dfsVisitedBB.contains(b)) return;
        dfsVisitedBB.add(b);
        for(BasicBlock bb : b.succ)
            dfsReversePostOrder(bb);
        reversePostOrder.addFirst(b);
    }

    private void dfsReversePreOrder(BasicBlock b) {
        if(dfsVisitedBB.contains(b))
            return;
        dfsVisitedBB.add(b);
        for(BasicBlock bb : b.preds)
            dfsReversePreOrder(bb);
        reversePreOrder.addFirst(b);
    }

    private void dfsRecursiveUsedGlobalVars(IRFunc f) {
        if(dfsVisitedFunc.contains(f))
            return;
        dfsVisitedFunc.add(f);
        for(IRFunc func : f.calleeFuncs)
            dfsRecursiveUsedGlobalVars(func);
        recursiveUsedGlobalVars.addAll(f.usedGlobalVars);
    }

    private void calcReversePostOrder(){
        dfsVisitedBB = new HashSet<>();
        reversePostOrder.clear();
        dfsReversePostOrder(firstBB);
    }

    private void calcReversePreOrder(){
        dfsVisitedBB = new HashSet<>();
        reversePreOrder.clear();
        dfsReversePreOrder(lastBB);
    }

    private void calcRecursiveUsedGlobalVars(){
        dfsVisitedFunc = new HashSet<>();
        recursiveUsedGlobalVars.clear();
        dfsRecursiveUsedGlobalVars(this);
    }

    private LinkedList<PhysicalRegister> toPreg(LinkedList<IRRegister> regs){
        LinkedList<PhysicalRegister> pRegs = new LinkedList<>();
        for (IRRegister reg : regs)
            pRegs.add((PhysicalRegister) reg);
        return pRegs;
    }

    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
