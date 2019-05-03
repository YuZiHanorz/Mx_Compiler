package mxCompiler.Utility;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class ErrorTable {
    public List<String> errorList;

    public ErrorTable(){
        errorList = new LinkedList<>();
    }

    public void addError(Location location, String message){
        StackTraceElement[] stack = new Throwable().getStackTrace();
        String err = stack[1].getClassName() + "." + stack[1].getLineNumber() + ":" + location + ":" + message;
        errorList.add(err);
    }

    public void printTo(PrintStream out){
        StringBuilder str = new StringBuilder();
        for (String x : errorList)
            str.append(x + '\n');
        out.print(str.toString());
    }

    public boolean somethingWrong(){
        return !errorList.isEmpty();
    }

}
