
import compilerTools.CodeBlock;
import compilerTools.ErrorLSSL;
import compilerTools.Functions;
import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.Token;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private ArrayList<ErrorLSSL> errores;
    private ArrayList<Production> producciones;
    private HashMap<String, String> identificadores;

    public CompiladorController() {
        this.tokens = new ArrayList();
        this.errores = new ArrayList();
        this.producciones = new ArrayList();
        this.identificadores = new HashMap();
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public ArrayList<ErrorLSSL> getErrores() {
        return errores;
    }

    public ArrayList<Production> getProducciones() {
        return producciones;
    }

    public void compilar(byte[] codigoFuente, String nombreArchivoFuente) {
        resetearCompilador();
        analisisLexico(codigoFuente);
        analisisSintactico();
        analisisSemantico();

        if (errores.isEmpty()) {
            generarArchivoSalida(nombreArchivoFuente);
        }
    }

    private void resetearCompilador() {
        tokens.clear();
        errores.clear();
        producciones.clear();
        identificadores.clear();
    }

    private void analisisLexico(byte[] bytesCodeText) {
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

    private void analisisSintactico() {
        Grammar gramatica = new Grammar(tokens, errores);

        // Realiza las agrupaciones de la gramatica
        eliminarTokensDeErrorLexico(gramatica);

        agruparExpresionesAlgebraicas(gramatica);

        // definirArreglos(gramatica);
        // definirCaracteres(gramatica);
        definirVariablesNumericas(gramatica);
        definirAsignacionesVariables(gramatica);

        agruparExpresionesLogicas(gramatica);
        agruparEstructurasCondicionales(gramatica);
        agruparEstructurasIterativas(gramatica);
        agruparLlamadosFunciones(gramatica);
        agruparSentencias(gramatica);

        eliminarDelimitadores(gramatica);

        // Muestra las agrupaciones realizadas en la consola
        gramatica.show();
    }

    private void eliminarTokensDeErrorLexico(Grammar gramatica) {
        gramatica.delete("ERROR_1", 1,
                "Error léxico {}: el número '[]' no está definido correctamente [#, %]");
        gramatica.delete("ERROR_2", 2,
                "Error léxico {}: el valor '[]' no es un token válido [#, %]");
    }

    private void agruparExpresionesAlgebraicas(Grammar gramatica) {
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("OPERACION_ALGEBRAICA", "(NUMERO | OPERACION_ALGEBRAICA | IDENTIFICADOR) (OPERADOR_ALGEBRAICO) "
                    + "(NUMERO | OPERACION_ALGEBRAICA | IDENTIFICADOR)");
        });

        gramatica.delete("OPERADOR_ALGEBRAICO", 1, "Error sintactico {}: el operador algebraico '[]' no está en una declaración [#, %]");
    }

    private void definirArreglos(Grammar gramatica) {
        gramatica.group("ARREGLO_NUMERICO", "(INTEGER | FLOAT) IDENTIFICADOR CORCHETE_APERTURA (NUMERO)? CORCHETE_CIERRE "
                + "(OPERADOR_ASIGNACION LLAVE_APERTURA (NUMERO | (NUMERO (COMA NUMERO)+))? LLAVE_CIERRE)? PUNTO_COMA");

        gramatica.group("ARREGLO_CARACTERES", "CHAR IDENTIFICADOR CORCHETE_APERTURA (NUMERO)? CORCHETE_CIERRE "
                + "(OPERADOR_ASIGNACION LLAVE_APERTURA (CARACTER | (CARACTER (COMA CARACTER)+))? LLAVE_CIERRE)? PUNTO_COMA");
        gramatica.group("ARREGLO_CARACTERES", "CHAR IDENTIFICADOR CORCHETE_APERTURA (NUMERO)? CORCHETE_CIERRE "
                + "(OPERADOR_ASIGNACION TEXTO)? PUNTO_COMA");
    }

    private void definirCaracteres(Grammar gramatica) {
        gramatica.group("VALOR_NUMERICO", "(NUMERO | OPERACION_ALGEBRAICA)", true);
        gramatica.group("VARIABLE_CARACTER", "CHAR IDENTIFICADOR (OPERADOR_ASIGNACION (CARACTER | IDENTIFICADOR))? "
                + "PUNTO_COMA", true, producciones);

        gramatica.delete(new String[]{"CHAR"}, 9,
                "Error sintactico {}: el tipo de dato '[]' no está en una declaración válida [#, %]");
    }

    private void definirVariablesNumericas(Grammar gramatica) {
        gramatica.group("VALOR_NUMERICO", "(NUMERO | OPERACION_ALGEBRAICA)", true);

        gramatica.group("VARIABLE_NUMERICA", "INTEGER IDENTIFICADOR (OPERADOR_ASIGNACION (VALOR_NUMERICO | IDENTIFICADOR))? PUNTO_COMA", true, producciones);

        gramatica.delete(new String[]{"INTEGER"}, 2,
                "Error sintactico {}: el tipo de dato '[]' no está en una declaración válida [#, %]");
        gramatica.group("VARIABLE_NUMERICA", "INTEGER IDENTIFICADOR OPERADOR_ASIGNACION VALOR_NUMERICO", 3,
                "Error sintactico {}: falta ';' al final de la línea [#, %]");
        gramatica.group("VARIABLE_NUMERICA", "INTEGER IDENTIFICADOR", 4,
                "Error sintactico {}: falta ';' al final de la línea [#, %]");
    }

    private void definirAsignacionesVariables(Grammar gramatica) {
        gramatica.group("ASIGNACION_VARIABLE", "IDENTIFICADOR OPERADOR_ASIGNACION VALOR_NUMERICO PUNTO_COMA");

        gramatica.group("ASIGNACION_VARIABLE", "IDENTIFICADOR OPERADOR_ASIGNACION VALOR_NUMERICO", 6,
                "Error sintactico {}: falta ';' al final de la línea [#, %]");
    }

    private void agruparExpresionesLogicas(Grammar gramatica) {
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("EXPRESION_LOGICA", "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA) "
                    + "(OPERADOR_LOGICO | OPERADOR_RELACIONAL) "
                    + "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA)");

            gramatica.group("EXPRESION_LOGICA", "OPERADOR_NOT (IDENTIFICADOR | EXPRESION_LOGICA)");

            gramatica.group("EXPRESION_LOGICA", "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA | VALOR_NUMERICO) "
                    + "OPERADOR_RELACIONAL"
                    + "(IDENTIFICADOR | OPERADOR_BOOLEANO | EXPRESION_LOGICA | VALOR_NUMERICO)");
        });

        gramatica.delete(new String[]{"OPERADOR_BOOLEANO", "OPERADOR_LOGICO", "OPERADOR_RELACIONAL"}, 7,
                "Error sintactico {}: el operador lógico '[]' no está en una declaración válida [#, %]");
    }

    private void agruparEstructurasCondicionales(Grammar gramatica) {
        gramatica.group("ESTRUCTURA_CONDICIONAL_IF", "IF PARENTESIS_APERTURA EXPRESION_LOGICA PARENTESIS_CIERRE");
        gramatica.group("ESTRUCTURA_CONDICIONAL_ELSE_IF", "ELSE_IF PARENTESIS_APERTURA EXPRESION_LOGICA PARENTESIS_CIERRE");
        gramatica.group("ESTRUCTURA_CONDICIONAL_ELSE", "ELSE");

        gramatica.delete(new String[]{"IF", "ELSE_IF", "ELSE"}, 8,
                "Error sintactico {}: la estructura condicional '[]' no está en una declaración válida [#, %]");
    }

    private void agruparEstructurasIterativas(Grammar gramatica) {
        gramatica.group("INCREMENTO", "IDENTIFICADOR OPERADOR_INCREMENTO");
        gramatica.group("DECREMENTO", "IDENTIFICADOR OPERADOR_DECREMENTO");

        gramatica.group("ESTRUCTURA_REPETICION_FOR", "FOR PARENTESIS_APERTURA "
                + "VARIABLE_NUMERICA EXPRESION_LOGICA PUNTO_COMA (INCREMENTO | DECREMENTO) PARENTESIS_CIERRE");
        gramatica.group("ESTRUCTURA_REPETICION_WHILE", "WHILE PARENTESIS_APERTURA EXPRESION_LOGICA PARENTESIS_CIERRE");

        gramatica.delete(new String[]{"FOR", "WHILE"}, 9,
                "Error sintactico {}: la estructura repetitiva '[]' no está en una declaración válida [#, %]");
    }

    private void agruparLlamadosFunciones(Grammar gramatica) {
        gramatica.group("LLAMADO_FUNCION", "PRINTF PARENTESIS_APERTURA TEXTO (COMA IDENTIFICADOR)* PARENTESIS_CIERRE PUNTO_COMA");
        gramatica.group("LLAMADO_FUNCION", "SCANF PARENTESIS_APERTURA TEXTO (COMA AMPERSAND IDENTIFICADOR)+ PARENTESIS_CIERRE PUNTO_COMA");

        gramatica.delete(new String[]{"PRINTF", "SCANF"}, 10,
                "Error sintactico {}: el llamado a la función '[]' no está en una declaración válida [#, %]");
    }

    private void agruparSentencias(Grammar gramatica) {
        gramatica.loopForFunExecUntilChangeNotDetected(() -> {
            gramatica.group("SENTENCIA", "(VARIABLE_NUMERICA | ASIGNACION_VARIABLE | LLAMADO_FUNCION)");

            gramatica.group("ESTRUCTURA_CONDICIONAL_IF_COMPLETA", "ESTRUCTURA_CONDICIONAL_IF LLAVE_APERTURA (SENTENCIA)* LLAVE_CIERRE");
            gramatica.group("ESTRUCTURA_CONDICIONAL_ELSE_IF_COMPLETA", "ESTRUCTURA_CONDICIONAL_ELSE_IF LLAVE_APERTURA (SENTENCIA)* LLAVE_CIERRE");
            gramatica.group("ESTRUCTURA_CONDICIONAL_ELSE_COMPLETA", "ESTRUCTURA_CONDICIONAL_ELSE LLAVE_APERTURA (SENTENCIA)* LLAVE_CIERRE");

            gramatica.group("ESTRUCTURA_REPETICION_FOR_COMPLETA", "ESTRUCTURA_REPETICION_FOR LLAVE_APERTURA (SENTENCIA)* LLAVE_CIERRE");
            gramatica.group("ESTRUCTURA_REPETICION_WHILE_COMPLETA", "ESTRUCTURA_REPETICION_WHILE LLAVE_APERTURA (SENTENCIA)* LLAVE_CIERRE");

            gramatica.group("SENTENCIA", "(SENTENCIA | ESTRUCTURA_CONDICIONAL_IF_COMPLETA | "
                    + "ESTRUCTURA_CONDICIONAL_ELSE_IF_COMPLETA | ESTRUCTURA_CONDICIONAL_ELSE_COMPLETA"
                    + " | ESTRUCTURA_REPETICION_FOR_COMPLETA | ESTRUCTURA_REPETICION_WHILE_COMPLETA)");
        });

        gramatica.delete(new String[]{"ESTRUCTURA_CONDICIONAL_IF", "ESTRUCTURA_CONDICIONAL_ELSE_IF", "ESTRUCTURA_CONDICIONAL_ELSE"}, 8,
                "Error sintactico {}: la estructura condicional '[]' no está en una declaración válida [#, %]");
        gramatica.delete(new String[]{"ESTRUCTURA_REPETICION_FOR", "ESTRUCTURA_REPETICION_WHILE"}, 9,
                "Error sintactico {}: la estructura repetitiva '[]' no está en una declaración válida [#, %]");
    }

    private void eliminarDelimitadores(Grammar gramatica) {
        gramatica.delete(new String[]{"LLAVE_APERTURA", "LLAVE_CIERRE"}, 11,
                "Error sintáctico {}: la llave '[]' no está contenida en una agrupación [#, %]");
        gramatica.delete(new String[]{"PARENTESIS_APERTURA", "PARENTESIS_CIERRE"}, 12,
                "Error sintáctico {}: el paréntesis '[]' no está contenido en una agrupación [#, %]");
        gramatica.delete(new String[]{"CORCHETE_APERTURA", "CORCHETE_CIERRE"}, 13,
                "Error sintáctico {}: el corchete '[]' no está contenido en una agrupación [#, %]");
    }

    private void analisisSemantico() {
        for (Production id : producciones) {
            switch (id.lexemeRank(0)) {
                case "int":
                    if (id.lexicalCompRank(0, -1).contains("OPERADOR_ASIGNACION")) {
                        if (!(id.lexicalCompRank(-2).equals("NUMERO") || id.lexicalCompRank(0, -1).contains("NUMERO OPERADOR_ALGEBRAICO NUMERO"))) {
                            errores.add(new ErrorLSSL(1, "Error semántico {}: valor no compatible con el tipo de dato [#, %]", id, true));
                        } else {
                            identificadores.put(id.lexemeRank(1), id.lexemeRank(-2));
                        }
                    }
                    break;
//                case "float":
//                    if (id.lexicalCompRank(0, -1).contains("OPERADOR_ASIGNACION")) {
//                        if (!(id.lexicalCompRank(-2).equals("NUMERO") || id.lexicalCompRank(0, -1).contains("NUMERO OPERADOR_ALGEBRAICO NUMERO"))) {
//                            errors.add(new ErrorLSSL(1, "Error semántico {}: valor no compatible con el tipo de dato [#, %]", id, true));
//                        } else {
//                            identificadores.put(id.lexemeRank(1), id.lexemeRank(-2));
//                        }
//                    }
//                    break;
//                case "char":
//                    if (id.lexicalCompRank(0, -1).contains("OPERADOR_ASIGNACION")) {
//                        if (!(id.lexicalCompRank(-2).equals("CARACTER"))) {
//                            errors.add(new ErrorLSSL(1, "Error semántico {}: valor no compatible con el tipo de dato [#, %]", id, true));
//                        } else {
//                            identificadores.put(id.lexemeRank(1), id.lexemeRank(-4, -2));
//                        }
//                    }
//                    break;
            }
        }
    }

    private void generarArchivoSalida(String fileName) {
        String rutaCarpeta = System.getProperty("user.dir") + "/src/salida";
        String nombreArchivo = convertirAPascalCase(fileName);
        String rutaArchivo = rutaCarpeta + "/" + nombreArchivo + ".java";

        try {
            // Crear la carpeta si no existe
            File carpeta = new File(rutaCarpeta);
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            // Crear un objeto FileWriter para escribir en el archivo
            FileWriter fileWriter = new FileWriter(rutaArchivo);

            // Crear un objeto BufferedWriter para escribir en el archivo de manera eficiente
            BufferedWriter writer = new BufferedWriter(fileWriter);

            // Agregar código Java al archivo
            writer.write("package salida;\n\n");
            writer.write("import java.util.Scanner;\n\n");
            writer.write("public class " + nombreArchivo + " {\n");
            writer.write(generarEspacios(4) + "public static void main(String[] args) {\n");
            writer.write(traducirCodigo());
            writer.write(generarEspacios(4) + "}\n");
            writer.write("}\n");

            // Cerrar el BufferedWriter y FileWriter
            writer.close();
            fileWriter.close();

            System.out.println("Archivo generado exitosamente.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String convertirAPascalCase(String nombreArchivo) {
        // Eliminar la extensión ".c" si está presente
        if (nombreArchivo.endsWith(".c")) {
            nombreArchivo = nombreArchivo.substring(0, nombreArchivo.length() - 2);
        }

        // Dividir el nombre en palabras utilizando "-", "_" y mayúsculas como separadores
        String[] partesArchivo = nombreArchivo.split("[-_A-Z]");

        StringBuilder nombrePascalCase = new StringBuilder();
        for (String parte : partesArchivo) {
            if (!parte.isEmpty()) {
                // Convertir la primera letra de cada palabra a mayúscula
                nombrePascalCase.append(Character.toUpperCase(parte.charAt(0)));
                if (parte.length() > 1) {
                    nombrePascalCase.append(parte.substring(1).toLowerCase());
                }
            }
        }

        return nombrePascalCase.toString();
    }

    private String traducirCodigo() {
        CodeBlock bloquesCodigo = Functions.splitCodeInCodeBlocks(tokens, "{", "}", ";");
        ArrayList<String> bloquesCodigoOrdenados = bloquesCodigo.getBlocksOfCodeInOrderOfExec();

        return manejarTraduccionCodigo(bloquesCodigoOrdenados, 2);
    }

    private String manejarTraduccionCodigo(ArrayList<String> bloquesCodigo, int nivel) {
        String codigoTraducido = "";
        int[] posicionMarcador;
        ArrayList<String> bloque;

        if (bloquesCodigo.isEmpty()) {
            return "";
        }

        String bloqueCodigoInicial = bloquesCodigo.get(0);
        if (bloqueCodigoInicial.startsWith("~") && bloqueCodigoInicial.length() == 38) {
            posicionMarcador = CodeBlock.getPositionOfBothMarkers(bloquesCodigo, bloqueCodigoInicial);
            bloque = new ArrayList<>(bloquesCodigo.subList(posicionMarcador[0] + 1, posicionMarcador[1]));

            codigoTraducido += manejarTraduccionCodigo(bloque, nivel);
        } else {
            ArrayList<String> sentencias = separarSentencias(bloqueCodigoInicial);

            for (String sentencia : sentencias) {
                sentencia = sentencia.trim();

                if (sentencia.startsWith("int")) {
                    codigoTraducido += generarEspacios(nivel * 4) + sentencia + ";\n";
                } else if (sentencia.startsWith("for") || sentencia.startsWith("while")) {
                    if (bloquesCodigo.size() > 1) {
                        String siguienteBloqueCodigo = bloquesCodigo.get(1);
                        posicionMarcador = CodeBlock.getPositionOfBothMarkers(bloquesCodigo, siguienteBloqueCodigo);
                        bloque = new ArrayList<>(bloquesCodigo.subList(posicionMarcador[0] + 1, posicionMarcador[1]));

                        codigoTraducido += generarEspacios(nivel * 4) + sentencia + " {\n";
                        codigoTraducido += manejarTraduccionCodigo(bloque, nivel + 1);
                        codigoTraducido += generarEspacios(nivel * 4) + "}\n";
                    } else {
                        codigoTraducido += generarEspacios(nivel * 4) + sentencia + " { }\n";
                    }
                } else if (sentencia.startsWith("if")) {
                    if (bloquesCodigo.size() > 1) {
                        String siguienteBloqueCodigo = bloquesCodigo.get(1);
                        posicionMarcador = CodeBlock.getPositionOfBothMarkers(bloquesCodigo, siguienteBloqueCodigo);
                        bloque = new ArrayList<>(bloquesCodigo.subList(posicionMarcador[0] + 1, posicionMarcador[1]));

                        codigoTraducido += "\n" + generarEspacios(nivel * 4) + sentencia + " {\n";
                        codigoTraducido += manejarTraduccionCodigo(bloque, nivel + 1);

                        if (bloquesCodigo.size() > posicionMarcador[1] + 1) {
                            String siguienteBloqueCodigoIf = bloquesCodigo.get(posicionMarcador[1] + 1);
                            int[] posicionMarcadorIf = {};

                            while (siguienteBloqueCodigoIf.contains("else if") || siguienteBloqueCodigoIf.contains("else")) {
                                if (posicionMarcadorIf.length == 0 && bloquesCodigo.size() > posicionMarcador[1] + 2) {
                                    posicionMarcadorIf = CodeBlock.getPositionOfBothMarkers(bloquesCodigo,
                                            bloquesCodigo.get(posicionMarcador[1] + 2));
                                } else if (posicionMarcadorIf.length > 0 && bloquesCodigo.size() > posicionMarcadorIf[1] + 2) {
                                    posicionMarcadorIf = CodeBlock.getPositionOfBothMarkers(bloquesCodigo,
                                            bloquesCodigo.get(posicionMarcadorIf[1] + 2));
                                } else {
                                    break;
                                }

                                ArrayList<String> bloqueIf = new ArrayList<>(bloquesCodigo.subList(posicionMarcadorIf[0] - 1, posicionMarcadorIf[1] + 1));
                                codigoTraducido += generarEspacios(nivel * 4) + "} " + manejarTraduccionCodigo(bloqueIf, nivel);
                            }
                            
                            codigoTraducido += generarEspacios(nivel * 4) + "} \n";

                            if (bloquesCodigo.size() > posicionMarcadorIf[1] + 1) {
                                ArrayList bloqueDespuesIf = new ArrayList<>(bloquesCodigo.subList(posicionMarcadorIf[1] + 1, bloquesCodigo.size()));
                                codigoTraducido += manejarTraduccionCodigo(bloqueDespuesIf, nivel);
                            }
                        }
                    } else {
                        codigoTraducido += generarEspacios(nivel * 4) + sentencia + " { } \n";
                    }
                } else if (sentencia.startsWith("else") || sentencia.startsWith("else if")) {
                    if (bloquesCodigo.size() > 1) {
                        String siguienteBloqueCodigo = bloquesCodigo.get(1);
                        posicionMarcador = CodeBlock.getPositionOfBothMarkers(bloquesCodigo, siguienteBloqueCodigo);
                        bloque = new ArrayList<>(bloquesCodigo.subList(posicionMarcador[0] + 1, posicionMarcador[1]));

                        codigoTraducido += sentencia + " {\n";
                        codigoTraducido += manejarTraduccionCodigo(bloque, nivel + 1);
                    } else {
                        codigoTraducido += sentencia + " { }\n";
                    }
                } else if (sentencia.startsWith("printf")) {
                    String sentenciaTraducida = sentencia.replace("printf", "System.out.printf");
                    codigoTraducido += generarEspacios(nivel * 4) + sentenciaTraducida + ";\n";
                } else if (sentencia.startsWith("scanf")) {
                    String sentenciaTraducida = convertirScanfACodigoJava(sentencia.replace(" ", ""));
                    codigoTraducido += sentenciaTraducida;
                } else if (!sentencia.isEmpty()) {
                    codigoTraducido += generarEspacios(nivel * 4) + sentencia + ";\n";
                }
            }
        }

        return codigoTraducido;
    }

    public String generarEspacios(int cantEspacios) {
        String resultado = "";
        for (int i = 0; i < cantEspacios; i++) {
            resultado += " ";
        }
        return resultado;
    }

    public static ArrayList<String> separarSentencias(String sentencias) {
        ArrayList<String> sentenciasSeparadas = new ArrayList<>();
        Pattern pattern = Pattern.compile("for\\s*\\([^)]*\\)|;");
        Matcher matcher = pattern.matcher(sentencias);

        int start = 0;
        while (matcher.find()) {
            String match = matcher.group();
            if (match.equals(";")) {
                sentenciasSeparadas.add(sentencias.substring(start, matcher.start()));
                start = matcher.end();
            }
        }

        sentenciasSeparadas.add(sentencias.substring(start));

        return sentenciasSeparadas;
    }

    private String convertirScanfACodigoJava(String codigoC) {
        String parametros = codigoC.substring(7, codigoC.length() - 1);
        String[] parametrosVariables = parametros.split(",");
        String codigoJava = "";

        for (int i = 1; i < parametrosVariables.length; i++) {
            String variable = parametrosVariables[i].substring(1);
            codigoJava += "\t" + variable + " = new Scanner(System.in).nextInt();\n";
        }

        return codigoJava;
    }
}
