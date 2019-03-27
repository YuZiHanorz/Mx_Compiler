package mxCompiler.Type;

import mxCompiler.Symbol.BaseTypeSymbol;

//includes int, bool, void
public class TypeBase extends TypeType {
    public String name = null;
    public BaseTypeSymbol symbol = null;

    public TypeBase(){}
    public TypeBase(String name, BaseTypeSymbol symbol){
        this.name = name;
        this.symbol = symbol;
    }

    @Override
    public boolean match(TypeType o) {
        if (o instanceof TypeBase) {
            String other = ((TypeBase) o).name;
            if (this.name.equals(other))
                return true;
            else return false;
        }
        else return false;
    }
}



