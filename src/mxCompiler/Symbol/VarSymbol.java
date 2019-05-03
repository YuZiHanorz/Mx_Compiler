package mxCompiler.Symbol;

import mxCompiler.IR.operand.VirtualRegister;
import mxCompiler.Type.TypeType;
import mxCompiler.Utility.Location;

public class VarSymbol {
    public String name;
    public TypeType type;
    public Location location;

    public boolean isGlobal;
    public boolean isClassMember;

    public VirtualRegister vR;

    public VarSymbol(String name, TypeType type, Location loc, boolean isGlobal, boolean isClassMember){
        this.name = name;
        this.type = type;
        this.location = loc;
        this.isGlobal = isGlobal;
        this.isClassMember = isClassMember;
    }


}
