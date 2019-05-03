package mxCompiler.Type;

import mxCompiler.Utility.Configuration;

public class TypeArray extends TypeType {
    public TypeType elementType = null;
    public int dim = -1;

    public TypeArray(){}

    public TypeArray(TypeType eT, int dim){
        this.elementType = eT;
        this.dim = dim;
    }

    @Override
    public boolean match(TypeType o){
        if(o instanceof TypeCustom){
            if (((TypeCustom)o).name.equals("null"))
                return true;
            else return false;
        }
        else if (o instanceof TypeArray){
            return this.dim == ((TypeArray) o).dim && this.elementType.match(((TypeArray) o).elementType);
        }
        else return false;
    }

    @Override
    public int getSize(){
        return Configuration.regSize;
    }

}
