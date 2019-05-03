package mxCompiler.Symbol;

import mxCompiler.Utility.Location;

public class CustomTypeSymbol extends TypeSymbol {
    public String name = null; //class, string and null
    public Location location;
    public SymbolTable classSymbolTable = null;
}
