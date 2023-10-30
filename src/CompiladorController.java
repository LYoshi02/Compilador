
import compilerTools.ErrorLSSL;
import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.TextColor;
import compilerTools.Token;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Yoshi Debat
 */
public class CompiladorController {
    private ArrayList<Token> tokens;
    private ArrayList<ErrorLSSL> errors;
    private ArrayList<Production> identProd;
    private HashMap<String, String> identificadores;

    public CompiladorController() {
        this.tokens = new ArrayList();
        this.errors = new ArrayList();
        this.identProd = new ArrayList();
        this.identificadores = new HashMap();
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public ArrayList<ErrorLSSL> getErrors() {
        return errors;
    }

    public ArrayList<Production> getIdentProd() {
        return identProd;
    }
    
    public void compilar(byte[] bytesCodeText) {
        resetearCompilador();
        lexicalAnalysis(bytesCodeText);
        syntacticAnalysis();
//        semanticAnalysis();
    }
    
    private void resetearCompilador() {
        tokens.clear();
        errors.clear();
        identProd.clear();
        identificadores.clear();
    }
    
    private void lexicalAnalysis(byte[] bytesCodeText) {
        Lexer lexer;

        try {
            File codigo = new File("code.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            output.write(bytesCodeText);
            BufferedReader entrada = new BufferedReader(new InputStreamReader(new FileInputStream(codigo), "UTF-8"));
            lexer = new Lexer(entrada);
            while (true) {
                Token token = lexer.yylex();
                if (token == null) {
                    break;
                }
                tokens.add(token);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Compilador.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    private void syntacticAnalysis() {
        Grammar gramatica = new Grammar(tokens, errors);

        /* Eliminacion de errores */
        gramatica.delete(new String[]{"ERROR", "ERROR_1"}, 1);
        
        /* Agrupacion de operaciones algebraicas */
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("OPERACION_ALGEBRAICA", "PARENTESIS_APERTURA (NUMERO | OPERACION_ALGEBRAICA | IDENTIFICADOR) (OPERADOR_ALGEBRAICO) "
                    + "(NUMERO | OPERACION_ALGEBRAICA | IDENTIFICADOR) PARENTESIS_CIERRE");
            gramatica.group("OPERACION_ALGEBRAICA", "(NUMERO | OPERACION_ALGEBRAICA | IDENTIFICADOR) (OPERADOR_ALGEBRAICO) "
                    + "(NUMERO | OPERACION_ALGEBRAICA | IDENTIFICADOR)");
        });
        
        gramatica.group("VALOR_NUMERICO", "(NUMERO | OPERACION_ALGEBRAICA)", true);
        
        /* Declaración de variables */
        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR OPERADOR_ASIGNACION (VALOR_NUMERICO)", true, identProd);
        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR");
        
        gramatica.group("ASIGNACION_VARIABLE", "IDENTIFICADOR OPERADOR_ASIGNACION (VALOR_NUMERICO)");
        
        /* Eliminacion de operadores de asignacion */
        gramatica.delete("OPERADOR_ASIGNACION", 2, "Error sintactico {}: el operador de asignación no está en una declaración [#, %]");
         
        /* Agrupacion de expresiones logicas */
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
//            gramatica.group("EXPRESION_LOGICA", "PARENTESIS_APERTURA (IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA) "
//                    + "(OPERADOR_LOGICO | OPERADOR_RELACIONAL) "
//                    + "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA) PARENTESIS_CIERRE");
            gramatica.group("EXPRESION_LOGICA", "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA) "
                    + "(OPERADOR_LOGICO | OPERADOR_RELACIONAL) "
                    + "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA)");
            
            gramatica.group("EXPRESION_LOGICA", "OPERADOR_NOT (IDENTIFICADOR | EXPRESION_LOGICA)");
//            gramatica.group("EXPRESION_LOGICA", "PARENTESIS_APERTURA OPERADOR_NOT (IDENTIFICADOR | EXPRESION_LOGICA) PARENTESIS_CIERRE");
            
//            gramatica.group("EXPRESION_LOGICA", "PARENTESIS_APERTURA (IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA | VALOR_NUMERICO) "
//                    + "OPERADOR_RELACIONAL "
//                    + "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA | VALOR_NUMERICO) PARENTESIS_CIERRE");
            gramatica.group("EXPRESION_LOGICA", "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA | VALOR_NUMERICO) "
                    + "OPERADOR_RELACIONAL "
                    + "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA | VALOR_NUMERICO)");
        });
        
        /* Agrupacion de estructuras de repeticion */
        gramatica.group("INCREMENTO", "IDENTIFICADOR OPERADOR_INCREMENTO");
        gramatica.group("DECREMENTO", "IDENTIFICADOR OPERADOR_DECREMENTO");
        gramatica.group("ESTRUCTURA_REPETICION_FOR", "FOR PARENTESIS_APERTURA "
                + "VARIABLE PUNTO_COMA EXPRESION_LOGICA PUNTO_COMA (INCREMENTO | DECREMENTO)"
                + "PARENTESIS_CIERRE");
        gramatica.group("ESTRUCTURA_REPETICION_WHILE", "WHILE PARENTESIS_APERTURA EXPRESION_LOGICA PARENTESIS_CIERRE");

        /* Agrupacion de estructuras condicionales */
        gramatica.group("ESTRUCTURA_CONDICIONAL_ELSE_IF", "LLAVE_CIERRE ELSE_IF PARENTESIS_APERTURA EXPRESION_LOGICA PARENTESIS_CIERRE LLAVE_APERTURA");
        gramatica.group("ESTRUCTURA_CONDICIONAL_IF", "IF PARENTESIS_APERTURA EXPRESION_LOGICA PARENTESIS_CIERRE LLAVE_APERTURA");
        gramatica.group("ESTRUCTURA_CONDICIONAL_ELSE", "LLAVE_CIERRE ELSE LLAVE_APERTURA");
        
        /* Agrupación de Sentencias */
        gramatica.group("VARIABLE_PUNTO_COMA", "VARIABLE PUNTO_COMA", true);
        gramatica.group("ASIGNACION_VARIABLE_PUNTO_COMA", "ASIGNACION_VARIABLE PUNTO_COMA", true);
        gramatica.group("SENTENCIAS", "(VARIABLE_PUNTO_COMA | ASIGNACION_VARIABLE_PUNTO_COMA)+");
        
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("ESTRUCTURA_REPETICION_COMPLETA_CON_LLAVES",
                    "(ESTRUCTURA_REPETICION_FOR | ESTRUCTURA_REPETICION_WHILE) LLAVE_APERTURA (SENTENCIAS)? LLAVE_CIERRE", true);
            gramatica.group("SENTENCIAS", "(SENTENCIAS | ESTRUCTURA_REPETICION_COMPLETA_CON_LLAVES)+");
        });
        
        gramatica.show();
    }
    
    private void sampleSyntacticAnalysis() {
        Grammar gramatica = new Grammar(tokens, errors);

        /* Eliminacion de errores */
        gramatica.delete(new String[]{"ERROR", "ERROR_1", "ERROR_2"}, 1);

        /* Agrupacion de valores */
        gramatica.group("VALOR", "(NUMERO | COLOR)", true);

        /* Declaración de variables */
        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR OPERADOR_ASIGNACION VALOR", true, identProd);
        gramatica.group("VARIABLE", "TIPO_DATO OPERADOR_ASIGNACION VALOR", true, 2,
                "Error sintáctico {}: falta el identificador en la variable [#, %]");

        // Si un error se puede extender por varias lineas, este metodo marca el error en la ultima linea en que se produce
        gramatica.finalLineColumn();
        gramatica.group("VARIABLE", "TIPO_DATO IDENTIFICADOR OPERADOR_ASIGNACION", 3,
                "Error sintáctico {}: falta el valor en la declaración [#, %]");
        gramatica.initialLineColumn();

        /* Eliminacion de tipos de dato y operadores de asignacion */
        gramatica.delete("TIPO_DATO", 4, "Error sintactico {}: el tipo de dato no está en una declaración [#, %]");
        gramatica.delete("OPERADOR_ASIGNACION", 5, "Error sintactico {}: el operador de asignación no está en una declaración [#, %]");

        /* Agrupacion de identificadores y definicion de parametros */
        gramatica.group("VALOR", "IDENTIFICADOR", true);
        gramatica.group("PARAMETROS", "VALOR (COMA VALOR)+");

        /* Agrupacion de funciones */
        gramatica.group("FUNCION", "(MOVIMIENTO | PINTAR | DETENER_PINTAR | TOMAR | LANZAR_MONEDA | VER | DETENER_REPETIR)", true);
        gramatica.group("FUNCION_COMPLETA", "FUNCION PARENTESIS_APERTURA (VALOR | PARAMETROS)? PARENTESIS_CIERRE", true);
        gramatica.group("FUNCION_COMPLETA", "FUNCION (VALOR | PARAMETROS)? PARENTESIS_CIERRE", 6,
                "Error sintáctico {}: falta el paréntesis de apertura en la función [#, %]");
        gramatica.finalLineColumn();
        gramatica.group("FUNCION_COMPLETA", "FUNCION PARENTESIS_APERTURA (VALOR | PARAMETROS)", 7,
                "Error sintáctico {}: falta el paréntesis de cierre en la función [#, %]");
        gramatica.initialLineColumn();

        /* Eliminacion de funciones incompletas */
        gramatica.delete("FUNCION", 8, "Error sintáctico {}: la función no está declarada correctamente [#, %]");

        /* Agrupacion de expresiones logicas */
        // Este metodo permite ejecutar una funcion repetidas veces hasta que no varie el número de producciones
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("EXPRESION_LOGICA", "(FUNCION_COMPLETA | EXPRESION_LOGICA) (OPERADOR_LOGICO (FUNCION_COMPLETA | EXPRESION_LOGICA))+");
            gramatica.group("EXPRESION_LOGICA", "PARENTESIS_APERTURA (EXPRESION_LOGICA | FUNCION_COMPLETA) PARENTESIS_CIERRE");
        });

        /* Eliminacion de operadores logicos */
        gramatica.delete("OPERADOR_LOGICO", 9, "Error sintáctico {}: el operador lógico no está contenido en una expresión [#, %]");

        /* Agrupacion de expresiones logicas como valor y parametros */
        gramatica.group("VALOR", "EXPRESION_LOGICA");
        gramatica.group("PARAMETROS", "VALOR (COMA VALOR)+");

        /* Agrupacion de estructuras de control */
        gramatica.group("ESTRUCTURA_CONTROL", "(REPETIR | ESTRUCTURA_SI)");
        gramatica.group("ESTRUCTURA_CONTROL_COMPLETA", "ESTRUCTURA_CONTROL PARENTESIS_APERTURA PARENTESIS_CIERRE");
        gramatica.group("ESTRUCTURA_CONTROL_COMPLETA", "ESTRUCTURA_CONTROL (VALOR | PARAMETROS)");
        gramatica.group("ESTRUCTURA_CONTROL_COMPLETA", "ESTRUCTURA_CONTROL PARENTESIS_APERTURA (VALOR | PARAMETROS) PARENTESIS_CIERRE");

        /* Eliminacion de estructuras de control incompletas */
        gramatica.delete("ESTRUCTURA_CONTROL", 10, "Error sintáctico {}: la estructura de control no está declarada correctamente [#, %]");

        /* Eliminacion de paréntesis */
        gramatica.delete(new String[]{"PARENTESIS_APERTURA", "PARENTESIS_CIERRE"}, 11,
                "Error sintáctico {}: el paréntesis [] no está contenido en una agrupación [#, %]");

        /* Verificacion de punto y coma al final de una sentencia */
        gramatica.finalLineColumn();
        // Identificadores o variables
        gramatica.group("VARIABLE_PUNTO_COMA", "VARIABLE PUNTO_COMA", true);
        gramatica.group("VARIABLE_PUNTO_COMA", "VARIABLE", true, 12,
                "Error sintáctico {}: falta el punto y coma al final de la variable [#, %]");
        // Funciones
        gramatica.group("FUNCION_COMPLETA_PUNTO_COMA", "FUNCION_COMPLETA PUNTO_COMA", true);
        gramatica.group("FUNCION_COMPLETA_PUNTO_COMA", "FUNCION_COMPLETA", 13,
                "Error sintáctico {}: falta el punto y coma al final de la declaración de función [#, %]");
        gramatica.initialLineColumn();

        /* Eliminacion de punto y coma */
        gramatica.delete("PUNTO_COMA", 14, "Error sintáctico {}: el punto y coma no está al final de una sentencia [#, %]");

        /* Agrupación de sentencias */
        gramatica.group("SENTENCIAS", "(VARIABLE_PUNTO_COMA | FUNCION_COMPLETA_PUNTO_COMA)+");

        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("ESTRUCTURA_CONTROL_COMPLETA_CON_LLAVES",
                    "ESTRUCTURA_CONTROL_COMPLETA LLAVE_APERTURA (SENTENCIAS)? LLAVE_CIERRE", true);
            gramatica.group("SENTENCIAS", "(SENTENCIAS | ESTRUCTURA_CONTROL_COMPLETA_CON_LLAVES)+");
        });

        /* Estructuras de funcion incompletas */
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.initialLineColumn();
            gramatica.group("ESTRUCTURA_CONTROL_COMPLETA_CON_LLAVES", "ESTRUCTURA_CONTROL_COMPLETA (SENTENCIAS)? LLAVE_CIERRE",
                    true, 15, "Error sintáctico {}: falta la llave de apertura en la estructura de control [#, %]");

            gramatica.finalLineColumn();
            gramatica.group("ESTRUCTURA_CONTROL_COMPLETA_CON_LLAVES", "ESTRUCTURA_CONTROL_COMPLETA LLAVE_APERTURA SENTENCIAS",
                    true, 16, "Error sintáctico {}: falta la llave de cierre en la estructura de control [#, %]");

            gramatica.group("SENTENCIAS", "(SENTENCIAS | ESTRUCTURA_CONTROL_COMPLETA_CON_LLAVES)");
        });

        /* Eliminacion de llaves */
        gramatica.delete(new String[]{"LLAVE_APERTURA", "LLAVE_CIERRE"}, 17,
                "Error sintáctico {}: la llave [] no está contenida en una agrupación [#, %]");

        gramatica.show();
    }
    
    private void semanticAnalysis() {
        HashMap<String, String> identDataType = new HashMap<>();

        identDataType.put("color", "COLOR");
        identDataType.put("numero", "NUMERO");

        for (Production id : identProd) {
            if (!identDataType.get(id.lexemeRank(0)).equals(id.lexicalCompRank(-1))) {
                errors.add(new ErrorLSSL(1, "Error semántico {}: valor no compatible con el tipo de dato [#, %]", id, true));
            } else if (id.lexicalCompRank(-1).equals("COLOR") && !id.lexemeRank(-1).matches("#[0-9a-fA-F]+")) {
                errors.add(new ErrorLSSL(2, "Error semántico {}: el color no es un número hexadecimal [#, %]", id, false));
            } else {
                identificadores.put(id.lexemeRank(1), id.lexemeRank(-1));
            }
            System.out.println(id.lexemeRank(0, -1));
            System.out.println(id.lexicalCompRank(0, -1));
        }
    }
}
