package mxCompiler.IR.node;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.operand.StaticData;

import java.util.LinkedList;
import java.util.List;

public class IRProgram {
    public List<IRFunc> IRFuncList;
    public List<StaticData> staticDataList;

    public IRProgram(){
        IRFuncList = new LinkedList<>();
        staticDataList = new LinkedList<>();
    }

    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }

}
