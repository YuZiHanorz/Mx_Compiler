package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;

import java.util.LinkedList;
import java.util.List;

public class BasicBlock{
    public String BBName = null;
    public IRInst firstInst = null;
    public IRInst lastInst = null;
    public IRFunc parentFunc = null;
    public List<BasicBlock> preds = null;
    public List<BasicBlock> succ = null;

    public BasicBlock(IRFunc func, String BBName){
        func.sonBB.add(this);
        this.parentFunc = func;
        this.BBName = BBName;
        this.preds = new LinkedList<>();
        this.succ = new LinkedList<>();
    }

    public void pushHeadInst(IRInst i){
        if (firstInst == null) {
            firstInst = lastInst = i;
            i.prevInst = i.nxtInst = null;
        }
        else firstInst.prependInst(i);
    }

    public void pushTailInst(IRInst i){
        if (firstInst == null) {
            firstInst = lastInst = i;
            i.prevInst = i.nxtInst = null;
        }
        else if (lastInst instanceof IRBranch || lastInst instanceof IRJump || lastInst instanceof  IRReturn){
            // do nothing
        }
        else lastInst.appendInst(i);
    }

    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }


}
