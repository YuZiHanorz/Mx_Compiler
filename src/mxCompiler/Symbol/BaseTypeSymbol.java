package mxCompiler.Symbol;

public class BaseTypeSymbol extends TypeSymbol {
    public String name = null; //int, bool, void

    public BaseTypeSymbol(){}

    public BaseTypeSymbol(String name){
        this.name = name;
    }
}
