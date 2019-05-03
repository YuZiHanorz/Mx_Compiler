package mxCompiler.Symbol;

import mxCompiler.Type.TypeBase;
import mxCompiler.Type.TypeCustom;
import mxCompiler.Type.TypeType;
import mxCompiler.Utility.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class GlobalSymbolTable extends SymbolTable{
    public Map<String, BaseTypeSymbol> baseTypeMap;
    public Map<String, CustomTypeSymbol> customTypeMap;

    public HashSet<VarSymbol> globalinitVarSet;


    public GlobalSymbolTable(){
        super(null);
        baseTypeMap = new LinkedHashMap<>();
        customTypeMap = new LinkedHashMap<>();
        globalinitVarSet = new HashSet<>();
        defaultInit();
    }

    private TypeType typeInt(){
        BaseTypeSymbol symbol = baseTypeMap.get("int");
        return new TypeBase("int", symbol);
    }

    private TypeType typeBool(){
        BaseTypeSymbol symbol = baseTypeMap.get("bool");
        return new TypeBase("bool", symbol);
    }

    private TypeType typeVoid(){
        BaseTypeSymbol symbol = baseTypeMap.get("void");
        return new TypeBase("void", symbol);
    }

    private TypeType typeString(){
        CustomTypeSymbol symbol = customTypeMap.get("string");
        return new TypeCustom("string", symbol);
    }

    private TypeType typeNull(){
        CustomTypeSymbol symbol = customTypeMap.get("null");
        return new TypeCustom("null", symbol);
    }

    private FuncSymbol printBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "print";
        f.isGlobalFunc = true;
        f.location = new Location(0, 0);
        f.parameterTypeList.add(typeString());
        f.parameterNameList.add("str");
        f.returnType = typeVoid();
        return f;
    }

    private FuncSymbol printlnBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "println";
        f.isGlobalFunc = true;
        f.location = new Location(0, 0);
        f.parameterTypeList.add(typeString());
        f.parameterNameList.add("str");
        f.returnType = typeVoid();
        return f;
    }

    private FuncSymbol getStringBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "getString";
        f.isGlobalFunc = true;
        f.location = new Location(0,0);
        f.returnType = typeString();
        return f;
    }

    private FuncSymbol getIntBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "getInt";
        f.isGlobalFunc = true;
        f.location = new Location(0,0);
        f.returnType = typeInt();
        return f;
    }

    private FuncSymbol toStringBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "toString";
        f.isGlobalFunc = true;
        f.location = new Location(0, 0);
        f.parameterTypeList.add(typeInt());
        f.parameterNameList.add("i");
        f.returnType = typeString();
        return f;
    }

    private FuncSymbol lengthStringBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "string.length";
        f.isGlobalFunc = true;
        f.location = new Location(0,0);
        f.parameterTypeList.add(typeString());
        f.parameterNameList.add("this");
        f.returnType = typeInt();
        return f;
    }

    private FuncSymbol substringStringBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "string.substring";
        f.isGlobalFunc = true;
        f.location = new Location(0,0);
        f.parameterTypeList.add(typeString());
        f.parameterTypeList.add(typeInt());
        f.parameterTypeList.add(typeInt());
        f.parameterNameList.add("this");
        f.parameterNameList.add("left");
        f.parameterNameList.add("right");
        f.returnType = typeString();
        return f;
    }

    private FuncSymbol parseIntStringBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "string.parseInt";
        f.isGlobalFunc = true;
        f.location = new Location(0,0);
        f.parameterTypeList.add(typeString());
        f.parameterNameList.add("this");
        f.returnType = typeInt();
        return f;
    }

    private FuncSymbol ordStringBIF(){
        FuncSymbol f = new FuncSymbol();
        f.name = "string.ord";
        f.isGlobalFunc = true;
        f.location = new Location(0,0);
        f.parameterTypeList.add(typeString());
        f.parameterTypeList.add(typeInt());
        f.parameterNameList.add("this");
        f.parameterNameList.add("pos");
        f.returnType = typeInt();
        return f;
    }

    private void defaultInit(){
        baseTypeMap.put("int", new BaseTypeSymbol("int"));
        baseTypeMap.put("bool", new BaseTypeSymbol("bool"));
        baseTypeMap.put("void", new BaseTypeSymbol("void"));

        CustomTypeSymbol Tnull = new CustomTypeSymbol();
        Tnull.name = "null";
        Tnull.location = new Location(0,0);
        Tnull.classSymbolTable = new SymbolTable(this);
        putCustomType("null", Tnull);

        CustomTypeSymbol Tstring = new CustomTypeSymbol();
        Tstring.name = "string";
        Tstring.location = new Location(0,0);
        Tstring.classSymbolTable = new SymbolTable(this);
        //wtf: first put class<string> into map, then add Func<substring>
        putCustomType("string", Tstring);
        Tstring.classSymbolTable.putFunc("length", lengthStringBIF());
        Tstring.classSymbolTable.putFunc("substring", substringStringBIF());
        Tstring.classSymbolTable.putFunc("parseInt", parseIntStringBIF());
        Tstring.classSymbolTable.putFunc("ord", ordStringBIF());

        putFunc("print", printBIF());
        putFunc("println", printlnBIF());
        putFunc("getString", getStringBIF());
        putFunc("getInt", getIntBIF());
        putFunc("toString", toStringBIF());
    }


    public BaseTypeSymbol getBaseType(String name){
        return baseTypeMap.get(name);
    }

    public CustomTypeSymbol getCustomType(String name){
        return customTypeMap.get(name);
    }

    //public void putBaseType(String name, BaseTypeSymbol symbol){baseTypeMap.put(name, symbol);}

    public void putCustomType(String name, CustomTypeSymbol sybol){
        customTypeMap.put(name, sybol);
    }



}
