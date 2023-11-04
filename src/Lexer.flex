import compilerTools.Token;

%%
%class Lexer
%type Token
%line
%column
%{
    private Token token(String lexeme, String lexicalComp, int line, int column) {
        return new Token(lexeme, lexicalComp, line+1, column+1);
    }
%}*

/* -- EXPRESIONES REGULARES -- */
Letra = [A-Za-z_]
Digito = [0-9]
TerminadorDeLinea = \r|\n|\r\n
EntradaDeCaracter = [^\r\n]
EspacioEnBlanco = {TerminadorDeLinea} | [ \t\f]
Numero = 0 | [1-9][0-9]*

/* Comentario */
ComentarioTradicional = "/*" [^*] ~"*/" | "/*" "*"+ "/"
FinDeLineaComentario = "//" {EntradaDeCaracter}* {TerminadorDeLinea}?
ContenidoComentario = ( [^*] | \*+ [^/*] )*
ComentarioDeDocumentacion = "/**" {ContenidoComentario} "*"+ "/"
Comentario = {ComentarioTradicional} | {FinDeLineaComentario} | {ComentarioDeDocumentacion}
%%

/* -- IGNORAR -- */
{Comentario}|{EspacioEnBlanco} { /*Ignorar*/ }


/* -- TIPOS DE DATOS -- */
int { return token(yytext(), "INTEGER", yyline, yycolumn); }
char { return token(yytext(), "CHAR", yyline, yycolumn); }
float { return token(yytext(), "FLOAT", yyline, yycolumn); }

/* -- PALABRAS RESERVADAS -- */
/* Bucles */
for { return token(yytext(), "FOR", yyline, yycolumn); }
do { return token(yytext(), "DO", yyline, yycolumn); }
while { return token(yytext(), "WHILE", yyline, yycolumn); }
break { return token(yytext(), "BREAK", yyline, yycolumn); }

/* Condicionales */
if { return token(yytext(), "IF", yyline, yycolumn); }
"else if" { return token(yytext(), "ELSE_IF", yyline, yycolumn); }
else { return token(yytext(), "ELSE", yyline, yycolumn); }


/* -- DELIMITADORES -- */
\( { return token(yytext(), "PARENTESIS_APERTURA", yyline, yycolumn); }
\) { return token(yytext(), "PARENTESIS_CIERRE", yyline, yycolumn); }
\{ { return token(yytext(), "LLAVE_APERTURA", yyline, yycolumn); }
\} { return token(yytext(), "LLAVE_CIERRE", yyline, yycolumn); }
\[ { return token(yytext(), "CORCHETE_APERTURA", yyline, yycolumn); }
\] { return token(yytext(), "CORCHETE_CIERRE", yyline, yycolumn); }


/* -- OPERADORES -- */
/* Operadores algebraicos */
\+ |
\- |
\* |
\/ { return token(yytext(), "OPERADOR_ALGEBRAICO", yyline, yycolumn); }

/* Operadores Incremento y decremento */
\+\+ { return token(yytext(), "OPERADOR_INCREMENTO", yyline, yycolumn); }
\-\- { return token(yytext(), "OPERADOR_DECREMENTO", yyline, yycolumn); }

/* Operadores Lógicos */
\&\& |
\|\| { return token(yytext(), "OPERADOR_LOGICO", yyline, yycolumn); }
\! { return token(yytext(), "OPERADOR_NOT", yyline, yycolumn); }

/* Operadores Relacionales */
\< |
\<\= |
\> |
\>\= |
\=\= | 
\!\= { return token(yytext(), "OPERADOR_RELACIONAL", yyline, yycolumn); }

/* Operadores Booleanos */
true | 
false { return token(yytext(), "OPERADOR_BOOLEANO", yyline, yycolumn); }

/* Operador de asignacion */
\= { return token(yytext(), "OPERADOR_ASIGNACION", yyline, yycolumn); }

/* Operador de atribucion */
\+\= |
\-\= |
\*\= |
\/\= { return token(yytext(), "OPERADOR_ATRIBUCION", yyline, yycolumn); }


/* -- FUNCIONES -- */
printf { return token(yytext(), "PRINTF", yyline, yycolumn); }
scanf { return token(yytext(), "SCANF", yyline, yycolumn); }


/* -- OTROS -- */
/* main */
main { return token(yytext(), "MAIN", yyline, yycolumn); }

/* Numero */
{Numero} { return token(yytext(), "NUMERO", yyline, yycolumn); }

/* Signos de puntuacion */
\. { return token(yytext(), "PUNTO", yyline, yycolumn); }
\, { return token(yytext(), "COMA", yyline, yycolumn); }
\; { return token(yytext(), "PUNTO_COMA", yyline, yycolumn); }

/* Comillas */
\' { return token(yytext(), "COMILLAS_SIMPLE", yyline, yycolumn); }
\" { return token(yytext(), "COMILLAS_DOBLE", yyline, yycolumn); }

/* Otros simbolos */
\& { return token(yytext(), "AMPERSAND", yyline, yycolumn); }
\% { return token(yytext(), "PORCIENTO", yyline, yycolumn); }

/* Identificador */
{Letra}({Letra}|{Digito})* { return token(yytext(), "IDENTIFICADOR", yyline, yycolumn); }


/* -- ERRORES DE ANÁLISIS -- */
/* Numero erroneo */
0{Numero} { return token(yytext(), "ERROR_1", yyline, yycolumn); }

. { return token(yytext(), "ERROR", yyline, yycolumn); }