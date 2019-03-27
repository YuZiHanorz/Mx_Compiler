/*a grammar description for the Mx* language*/

grammar Mx;

import CommonMxLexerRules;

program
    :   declaration* EOF
    ;

declaration
    :   functionDeclaration
    |   classDeclaration
    |   variableDeclaration
    ;

functionDeclaration
    :   typeType IDENTIFIER '(' parameterDeclarationList? ')' block;

classDeclaration
    :   CLASS IDENTIFIER '{' memberDeclaration* '}'
    ;

variableDeclaration
    :   typeType variableDeclarationList ';'
    ;

//type
typeType
    :   type ('[' dim ']')*
    ;


type
    :   token = VOID
    |   token = INT
    |   token = BOOL
    |   token = STRING
    |   token = IDENTIFIER
    ;

//parameter
parameterDeclarationList
    :   parameterDeclaration (',' parameterDeclaration)*
    ;

parameterDeclaration
    :   typeType IDENTIFIER
    ;

//variable
variableDeclarationList
    :   variableDeclarator (',' variableDeclarator)*
    ;

variableDeclarator
    :   IDENTIFIER ('=' expr)?
    ;

//classMember
memberDeclaration
    :   functionDeclaration
    |   variableDeclaration
    |   constructorDeclaration
    ;

constructorDeclaration
    :   IDENTIFIER '(' parameterDeclarationList? ')' block
    ;

//statement
block
    :   '{' statementList '}'
    ;

statementList
    :   statement*
    ;

statement
    :   block                               #blockStatement
    |   expr ';'                            #exprStatement
    |   IF '(' expr ')' thenStmt=statement 
            (ELSE elseStmt=statement)?      #ifStatement
    |   WHILE '(' expr ')' statement        #whileStatement
    |   FOR '(' forInit=expr? ';' 
            forCondition=expr? ';' 
            forStep=expr? ')' statement     #forStatement
    |   CONTINUE ';'                        #continueStatement
    |   BREAK ';'                           #breakStatement
    |   RETURN expr? ';'                    #returnStatement
    |   variableDeclaration                 #variableStatement
    |   ';'                                 #emptyStatement
    ;

//expression
expr
    :   expr bop='.' (IDENTIFIER | functionCall)    #memberExpr
    |   expr '[' expr ']'                           #subscriptExpr    
    |   functionCall                                #funcCallExpr
    |   NEW creator                                 #newExpr
    |   expr suffix=('++'|'--')                     #unaryExpr
    |   prefix=('++'|'--') expr                     #unaryExpr
    |   prefix=('+'|'-') expr                       #unaryExpr
    |   prefix=('!'|'~') expr                       #unaryExpr
    |   expr bop=('*'|'/'|'%') expr                 #binaryExpr
    |   expr bop=('+'|'-') expr                     #binaryExpr
    |   expr bop=('<<'|'>>') expr                   #binaryExpr
    |   expr bop=('<'|'>') expr                     #binaryExpr
    |   expr bop=('<='|'>=') expr                   #binaryExpr
    |   expr bop=('=='|'!=') expr                   #binaryExpr
    |   expr bop='&' expr                           #binaryExpr
    |   expr bop='^' expr                           #binaryExpr
    |   expr bop='|' expr                           #binaryExpr
    |   expr bop='&&' expr                          #binaryExpr
    |   expr bop='||' expr                          #binaryExpr
    |   <assoc=right> expr bop='=' expr             #assignExpr
    |   primaryExpression                           #primaryExpr
    ;

primaryExpression
    :   '(' expr ')'
    |   token = IDENTIFIER
    |   token = THIS
    |   token = IntLiteral
    |   token = StringLiteral
    |   token = BoolLiteral
    |   token = NullLiteral
    ;

functionCall
    :   IDENTIFIER '(' parameterList? ')'
    ;

parameterList
    :   expr (',' expr)*
    ;

creator
    :   type (('[' expr ']')* ('[' dim ']')* | ('(' ')'))
    ;

//dimension counter
dim
    :;
