package mxCompiler.Parser;

import mxCompiler.Utility.ErrorTable;
import mxCompiler.Utility.Location;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SyntaxErrorListener extends BaseErrorListener {
    public ErrorTable errorTable;

    public SyntaxErrorListener(ErrorTable err){
        this.errorTable = err;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        errorTable.addError(new Location(line, charPositionInLine), msg);
    }

}
