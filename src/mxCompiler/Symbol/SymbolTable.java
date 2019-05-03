package mxCompiler.Symbol;

import mxCompiler.Utility.Configuration;
import org.antlr.v4.misc.OrderedHashMap;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    public SymbolTable Fa;
    public List<SymbolTable> children;

    public Map<String, VarSymbol> varMap;
    public Map<String, FuncSymbol> funcMap;

    public Map<String, Integer> varOffsetMap;
    private Integer curOffset = 0;

    public SymbolTable(SymbolTable fa){
        this.Fa = fa;
        this.children = new LinkedList<>();
        this.varMap = new LinkedHashMap<>();
        this.funcMap = new LinkedHashMap<>();
        this.varOffsetMap = new OrderedHashMap<>();
    }

    public void putVar(String name, VarSymbol var){
        varMap.put(name, var);
        varOffsetMap.put(name, curOffset);
        curOffset += Configuration.regSize;
    }

    public VarSymbol getVar(String name){
        return varMap.get(name);
    }

    public void putFunc(String name, FuncSymbol func){
        funcMap.put(name, func);
    }

    public FuncSymbol getFunc(String name){
        return funcMap.get(name);
    }

    public int getVarOffset(String name){
        return varOffsetMap.get(name);
    }

}
