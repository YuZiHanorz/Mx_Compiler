package mxCompiler.Type;

import mxCompiler.Symbol.CustomTypeSymbol;
import mxCompiler.Utility.Configuration;

//includes class, string and null
public class TypeCustom extends TypeType {
    public String name = null;
    public CustomTypeSymbol symbol = null;

    public TypeCustom(){}
    public TypeCustom(String name, CustomTypeSymbol symbol){
        this.name = name;
        this.symbol = symbol;
    }

    @Override
    public boolean match(TypeType o) {
        if (o instanceof TypeCustom) {
            String other = ((TypeCustom) o).name;
            if (this.name.equals(other))
                return true;
            else if (other.equals("null") && this.name.equals("string"))
                return false;
            else if (other.equals("string") && this.name.equals("null"))
                return false;
            else return other.equals("null") || this.name.equals("null");
        }
        else return false;
    }

    @Override
    public int getSize(){
        return Configuration.regSize * symbol.classSymbolTable.varMap.values().size();
    }
}
