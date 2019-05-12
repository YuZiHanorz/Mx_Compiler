package mxCompiler;

import mxCompiler.Ast.node.ProgramNode;
import mxCompiler.Backend.*;
import mxCompiler.Frontend.*;
import mxCompiler.IR.node.IRProgram;
import mxCompiler.Parser.MxLexer;
import mxCompiler.Parser.MxParser;
import mxCompiler.Parser.SyntaxErrorListener;
import mxCompiler.Symbol.GlobalSymbolTable;
import mxCompiler.Utility.Configuration;
import mxCompiler.Utility.ErrorTable;
import mxCompiler.Utility.RegCollection;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;
import java.util.Collection;

import static java.lang.System.err;
import static java.lang.System.exit;

public class MxCompiler {

    public static void main(String[] args) throws IOException {
        boolean debug = false;
        CharStream input;
        if (debug){
            String filename = "program.cpp";
            Configuration.fin = new FileInputStream(filename);
            //Configuration.printAST = true;
            Configuration.printIR = true;
            Configuration.printIRAfterRescan = true;
            Configuration.printIRAfterAllocate = true;
            Configuration.printAsmFile = true;
            input = CharStreams.fromStream(Configuration.fin); //debug
        }

        else {
            input = CharStreams.fromStream(System.in);
        }

        //compile
        ErrorTable errorTable = new ErrorTable();
        RegCollection.build();

        //build AST
        MxLexer mxLexer = new MxLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(mxLexer);
        MxParser parser = new MxParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener(errorTable));
        ParseTree tree = parser.program();

        checkError(errorTable);

        AstBuilder astBuilder = new AstBuilder(errorTable);
        ProgramNode ast = (ProgramNode) astBuilder.visit(tree);

        checkError(errorTable);

        if(Configuration.printAST) {
            System.err.println("------------------------");
            System.err.println("Print the AST:\n");
            AstPrinter astPrinter = new AstPrinter();
            astPrinter.visit(ast);
            astPrinter.printTo(System.err);
        }

        AstScopeChecker scopeChecker = new AstScopeChecker(errorTable);
        ast.accept(scopeChecker);

        checkError(errorTable);

        GlobalSymbolTable globalSymbolTable = scopeChecker.globalSYmbolTable;
        SemanticChecker semanticChecker = new SemanticChecker(globalSymbolTable, errorTable);
        ast.accept(semanticChecker);

        checkError(errorTable);

        if (Configuration.useConstFolderOpt) {
            ConstFolderOpt constFolderOpt = new ConstFolderOpt();
            ast.accept(constFolderOpt);
        }
        if (Configuration.useOutIrrelevantCodeOpt){
            OutputIrrelevantCodeOpt outputIrrelevantCodeOpt = new OutputIrrelevantCodeOpt();
            ast.accept(outputIrrelevantCodeOpt);
        }
        if (Configuration.useIrrelevantLoopOpt){
            IrrelevantLoopOpt irrelevantLoopOpt = new IrrelevantLoopOpt();
            ast.accept(irrelevantLoopOpt);
        }

        IRBuilder irBuilder = new IRBuilder(globalSymbolTable);
        ast.accept(irBuilder);
        IRProgram irProgram = irBuilder.irProgram;

        if (Configuration.printIR){
            System.err.println("------------------------");
            System.err.println("Print IR after building:\n");
            IRPrinter irPrinter = new IRPrinter();
            irPrinter.visit(irProgram);
            irPrinter.printTo(System.err);
        }
        if (Configuration.useConstPropagationOpt){
            ConstPropagationOpt constPropagationOpt = new ConstPropagationOpt();
            irProgram.accept(constPropagationOpt);
            if (Configuration.printIR){
                System.err.println("------------------------");
                System.err.println("Print IR after ConstPropagationOpt:\n");
                IRPrinter irPrinter = new IRPrinter();
                irPrinter.visit(irProgram);
                irPrinter.printTo(System.err);
            }
        }

        if (Configuration.useDeadInstRemoveOpt){
            DeadInstRemoveOpt deadInstRemoveOpt = new DeadInstRemoveOpt(irProgram);
            deadInstRemoveOpt.build();
        }


        IRRescanner irRescanner = new IRRescanner();
        irProgram.accept(irRescanner);

        if (Configuration.printIRAfterRescan){
            System.err.println("------------------------");
            System.err.println("Print IR after rescanning:\n");
            IRPrinter irPrinter = new IRPrinter();
            irPrinter.visit(irProgram);
            irPrinter.printTo(System.err);
        }
        RegisterAllocator registerAllocator = new RegisterAllocator(irProgram);
        registerAllocator.build();

        if (Configuration.printIRAfterAllocate){
            System.err.println("------------------------");
            System.err.println("Print IR after allocation:\n");
            IRPrinter irPrinter = new IRPrinter();
            irPrinter.visit(irProgram);
            irPrinter.printTo(System.err);
        }

        NASMTransformer nasmTransformer = new NASMTransformer(irProgram);
        nasmTransformer.build();

        if (Configuration.printAsmFile){
            System.err.println("------------------------");
            System.err.println("Print nasm finally:\n");
            NASMPrinter nasmPrinter = new NASMPrinter();
            nasmPrinter.visit(irProgram);
            nasmPrinter.printTo(new PrintStream("program.asm"));
        }
        NASMPrinter nasmPrinter = new NASMPrinter();
        nasmPrinter.visit(irProgram);
        nasmPrinter.printTo(System.out);
    }

    private static void checkError(ErrorTable errorTable){
        if (errorTable.somethingWrong()){
            errorTable.printTo(err);
            exit(1);
        }
    }


}
