package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;
import mxCompiler.Utility.Configuration;

public class StaticData extends IRConst{
    public String name = null;
    public String stringTypeInit = null; //for string type
    public int size;

    public StaticData(String name, int size){
        this.name = name;
        this.size = size;
    }


    public StaticData(String name, String stringInii){
        this.name = name;
        this.stringTypeInit = stringInii;
        this.size = stringInii.length() + Configuration.regSize + 1;
    }

    @Override
    public void accept(IRVisitor visitor){
        visitor.visit(this);
    }
}
