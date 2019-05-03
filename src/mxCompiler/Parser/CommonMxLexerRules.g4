//A lexer description for the Mx* language

lexer grammar CommonMxLexerRules;

BOOL        :       'bool';
INT         :       'int';
STRING      :       'string';
VOID        :       'void';
IF          :       'if';
ELSE        :       'else';
WHILE       :       'while';
FOR         :       'for';
BREAK       :       'break';
CONTINUE    :       'continue';
RETURN      :       'return';
NEW         :       'new';
CLASS       :       'class';
THIS        :       'this';

BoolLiteral     :   'true' | 'false';
NullLiteral     :   'null';
IntLiteral      :   '0' | [1-9][0-9]*;
StringLiteral   :   '"' (ESC | .)*?  '"';
fragment 
ESC     :   '\\"' | '\\\\';

IDENTIFIER  :       [a-zA-Z][0-9a-zA-Z_]*;
LINECOMMENT :       '//' .*? '\n'   -> skip;
BLOCKCOMMENT:       '/*' .*? '*/'   -> skip;
WS          :       [ \t\r\n]+      -> skip;



