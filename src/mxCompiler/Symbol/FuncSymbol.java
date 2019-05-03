package mxCompiler.Symbol;

import mxCompiler.Type.TypeType;
import mxCompiler.Utility.Location;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class FuncSymbol {
    public String name = null;
    public Location location = null;

    public List<String> parameterNameList;
    public List<TypeType> parameterTypeList;

    public HashSet<VarSymbol> globalVarSet;
    //public HashSet<FuncSymbol> funcCalleeSet;

    public boolean isGlobalFunc;

    public SymbolTable funcSymbolTable = null;

    public TypeType returnType = null;

    public FuncSymbol(){
        this.parameterNameList = new LinkedList<>();
        this.parameterTypeList = new LinkedList<>();
        this.globalVarSet = new HashSet<>();
        //this.funcCalleeSet = new HashSet<>();
    }


}
