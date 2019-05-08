package mxCompiler.Utility;

import java.io.FileInputStream;

public class Configuration {
    public static FileInputStream fin;

    public static boolean printAST = false;
    public static boolean printIR = false;
    public static boolean printIRAfterRescan = false;
    public static boolean printIRAfterAllocate = false;
    public static boolean printAsmFile = false;

    public static int regSize = 8;
    public static boolean simple = false;

    public static boolean useInlineOpt = false;
    public static int inlineMaxDepth = 3;
    public static int inlineOpCnt = 20;
}
