package mxCompiler.Utility;

import java.io.FileInputStream;
import java.io.PrintStream;

public class Configuration {
    public static FileInputStream fin;

    public static boolean printAST = false;
    public static boolean printIR = false;
    public static boolean printAsmFile = false;

    public static int regSize = 8;

    public static boolean useInlineOpt = true;
    public static int inlineMaxDepth = 4;
    public static int inlineOpCnt = 20;
}
