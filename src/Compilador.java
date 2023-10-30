
import com.formdev.flatlaf.FlatIntelliJLaf;
import compilerTools.CodeBlock;
import compilerTools.Directory;
import compilerTools.ErrorLSSL;
import compilerTools.Functions;
import compilerTools.Grammar;
import compilerTools.Production;
import compilerTools.TextColor;
import compilerTools.Token;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.swing.JOptionPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author yisus
 */
public class Compilador extends javax.swing.JFrame {

    private String windowTitle;
    private Directory directorio;
    private ArrayList<Token> tokens;
    private ArrayList<ErrorLSSL> errors;
    private ArrayList<TextColor> textsColor;
    private Timer timerKeyReleased;
    private ArrayList<Production> identProd;
    private HashMap<String, String> identificadores;
    private boolean codeHasBeenCompiled = false;

    /**
     * Creates new form Compilador
     */
    public Compilador() {
        initComponents();
        init();
    }

    private void init() {
        // Setear el titulo de la ventana al ejecutar el programa
        windowTitle = "Compiler Initial Setup";
        setTitle(windowTitle);
        // Centrar la ventana en la pantalla
        setLocationRelativeTo(null);

        directorio = new Directory(this, jtpCode, windowTitle, ".c");
        // Se ejecuta al intentar cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Pregunta si queremos guardar los cambios antes de cerrar la ventana
                directorio.Exit();
                System.exit(0);
            }
        });

        // Muestra los numeros de lineas en el editor de codigo
        Functions.setLineNumberOnJTextComponent(jtpCode);

        timerKeyReleased = new Timer((int) (1000 * 0.3), (ActionEvent e) -> {
            timerKeyReleased.stop();
        });

        // Agregar un asterisco en el nombre de la ventana cuando se edita codigo en el editor
        Functions.insertAsteriskInName(this, jtpCode, () -> {
            timerKeyReleased.restart();
        });
        // Agrega autocompletado en el editor de codigo presionanco ctrl+space
        String[] sampleKeywords = {"function", "String", "Integer", "private", "public"};
        Functions.setAutocompleterJTextComponent(sampleKeywords, jtpCode, () -> {
            timerKeyReleased.restart();
        });

        // Inicializacion de variables necesarias para el compilador
        tokens = new ArrayList<>();
        errors = new ArrayList<>();
        textsColor = new ArrayList<>();
        identProd = new ArrayList<>();
        identificadores = new HashMap<>();
    }

    private void clearFields() {
        Functions.clearDataInTable(tblTokens);
        jtaOutputConsole.setText("");
        tokens.clear();
        errors.clear();
        identProd.clear();
        identificadores.clear();
        codeHasBeenCompiled = false;
    }

    private void compile() {
        clearFields();
        lexicalAnalysis();
        fillTableTokens();
        syntacticAnalysis2();
        
//        semanticAnalysis();
        printConsole();
        codeHasBeenCompiled = true;
    }

    private void lexicalAnalysis() {
        Lexer lexer;

        try {
            File codigo = new File("code.encrypter");
            FileOutputStream output = new FileOutputStream(codigo);
            byte[] bytesText = jtpCode.getText().getBytes();
            output.write(bytesText);
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

    private void fillTableTokens() {
        tokens.forEach(token -> {
            Object[] data = new Object[]{token.getLexicalComp(), token.getLexeme(),
                "[" + token.getLine() + ", " + token.getColumn() + "]"};
            Functions.addRowDataInTable(tblTokens, data);
        });
    }

    private void syntacticAnalysis2() {
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
    
    private void syntacticAnalysis() {
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

    private void printConsole() {
        if (!errors.isEmpty()) {
            Functions.sortErrorsByLineAndColumn(errors);
            String strErrors = "\n";

            for (ErrorLSSL error : errors) {
                String strError = String.valueOf(error);
                strErrors += strError + "\n";
            }
            jtaOutputConsole.setText("Compilación terminada...\n" + strErrors + "\nLa Compilación terminó con errores...");
        } else {
            jtaOutputConsole.setText("Compilación terminada...");
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rootPanel = new javax.swing.JPanel();
        buttonsFilePanel = new javax.swing.JPanel();
        btnAbrir = new javax.swing.JButton();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnGuardarComo = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtpCode = new javax.swing.JTextPane();
        panelButtonCompilerExecute = new javax.swing.JPanel();
        btnCompilar = new javax.swing.JButton();
        btnEjecutar = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jtaOutputConsole = new javax.swing.JTextArea();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblTokens = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        btnAbrir.setText("Abrir");
        btnAbrir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAbrirActionPerformed(evt);
            }
        });

        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnGuardarComo.setText("Guardar como");
        btnGuardarComo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarComoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout buttonsFilePanelLayout = new javax.swing.GroupLayout(buttonsFilePanel);
        buttonsFilePanel.setLayout(buttonsFilePanelLayout);
        buttonsFilePanelLayout.setHorizontalGroup(
            buttonsFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsFilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnNuevo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAbrir)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnGuardar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnGuardarComo)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        buttonsFilePanelLayout.setVerticalGroup(
            buttonsFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(buttonsFilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(buttonsFilePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAbrir)
                    .addComponent(btnNuevo)
                    .addComponent(btnGuardar)
                    .addComponent(btnGuardarComo))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        jtpCode.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtpCodeKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jtpCode);

        btnCompilar.setText("Compilar");
        btnCompilar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCompilarActionPerformed(evt);
            }
        });

        btnEjecutar.setText("Ejecutar");
        btnEjecutar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEjecutarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelButtonCompilerExecuteLayout = new javax.swing.GroupLayout(panelButtonCompilerExecute);
        panelButtonCompilerExecute.setLayout(panelButtonCompilerExecuteLayout);
        panelButtonCompilerExecuteLayout.setHorizontalGroup(
            panelButtonCompilerExecuteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelButtonCompilerExecuteLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCompilar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnEjecutar)
                .addContainerGap())
        );
        panelButtonCompilerExecuteLayout.setVerticalGroup(
            panelButtonCompilerExecuteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelButtonCompilerExecuteLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelButtonCompilerExecuteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCompilar)
                    .addComponent(btnEjecutar))
                .addContainerGap(7, Short.MAX_VALUE))
        );

        jtaOutputConsole.setEditable(false);
        jtaOutputConsole.setBackground(new java.awt.Color(255, 255, 255));
        jtaOutputConsole.setColumns(20);
        jtaOutputConsole.setRows(5);
        jScrollPane2.setViewportView(jtaOutputConsole);

        tblTokens.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Componente léxico", "Lexema", "[Línea, Columna]"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblTokens.getTableHeader().setReorderingAllowed(false);
        jScrollPane3.setViewportView(tblTokens);

        javax.swing.GroupLayout rootPanelLayout = new javax.swing.GroupLayout(rootPanel);
        rootPanel.setLayout(rootPanelLayout);
        rootPanelLayout.setHorizontalGroup(
            rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rootPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, rootPanelLayout.createSequentialGroup()
                        .addComponent(buttonsFilePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(panelButtonCompilerExecute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 693, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 693, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
                .addGap(17, 17, 17))
        );
        rootPanelLayout.setVerticalGroup(
            rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rootPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buttonsFilePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(panelButtonCompilerExecute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(rootPanelLayout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 203, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap(8, Short.MAX_VALUE))
        );

        getContentPane().add(rootPanel);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        directorio.New();
        clearFields();
    }//GEN-LAST:event_btnNuevoActionPerformed


    private void btnAbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAbrirActionPerformed
        if (directorio.Open()) {
            clearFields();
        }
    }//GEN-LAST:event_btnAbrirActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        if (directorio.Save()) {
            clearFields();
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnGuardarComoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarComoActionPerformed
        if (directorio.SaveAs()) {
            clearFields();
        }
    }//GEN-LAST:event_btnGuardarComoActionPerformed

    private void btnCompilarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCompilarActionPerformed
        boolean fileNotSaved = getTitle().contains("*");
        boolean fileNotCreated = getTitle().equals(windowTitle);

        if (fileNotSaved || fileNotCreated) {
            if (directorio.Save()) {
                compile();
            }
        } else {
            compile();
        }
    }//GEN-LAST:event_btnCompilarActionPerformed

    private void btnEjecutarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEjecutarActionPerformed
        btnCompilar.doClick();
        if (codeHasBeenCompiled) {
            if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No se pudo ejecutar el codigo ya que se encontró uno o más");
            } else {
                CodeBlock codeBlock = Functions.splitCodeInCodeBlocks(tokens, "{", "}", ";");
                ArrayList<String> blocksOfCode = codeBlock.getBlocksOfCodeInOrderOfExec();
                executeCode(blocksOfCode, 1);
            }
        }
    }//GEN-LAST:event_btnEjecutarActionPerformed

    private void executeCode(ArrayList<String> blocksOfCode, int repeats) {
        for (int i = 1; i <= repeats; i++) {
            int repeatCode = -1;
            for (int j = 0; j < blocksOfCode.size(); j++) {
                String blockOfCode = blocksOfCode.get(j);
                if (repeatCode != -1) {
                    int[] posicionMarcador = CodeBlock.getPositionOfBothMarkers(blocksOfCode, blockOfCode);
                    executeCode(new ArrayList<>(blocksOfCode.subList(posicionMarcador[0], posicionMarcador[1])), repeatCode);
                    repeatCode = -1;
                    j = posicionMarcador[1];
                } else {
                    String[] sentences = blockOfCode.split(";");
                    for (String sentence : sentences) {
                        sentence = sentence.trim();
                        if (sentence.startsWith("pintar")) {
                            String parametro;
                            if (sentence.contains("$")) {
                                parametro = identificadores.get(sentence.substring(9, sentence.length() - 2));
                            } else {
                                parametro = sentence.substring(9, sentence.length() - 2);
                            }
                            System.out.println("Pintando de color " + parametro + "...");
                        } else if (sentence.startsWith("izquierda")) {
                            System.out.println("Moviendose a la izquierda...");
                        } else if (sentence.startsWith("derecha")) {
                            System.out.println("Moviendose a la derecha...");
                        } else if (sentence.startsWith("adelante")) {
                            System.out.println("Moviendose hacia adelante...");
                        } else if (sentence.startsWith("atras")) {
                            System.out.println("Moviendose hacia atras...");
                        } else if (sentence.contains("-->")) {
                            System.out.println("Declarando identificador...");
                        } else if (sentence.startsWith("repetir")) {
                            String parametro;
                            if (sentence.contains("$")) {
                                parametro = identificadores.get(sentence.substring(10, sentence.length() - 2));
                            } else {
                                parametro = sentence.substring(10, sentence.length() - 2);
                            }
                            repeatCode = Integer.parseInt(parametro);
                        }
                    }
                }
            }
        }
    }

    private void jtpCodeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtpCodeKeyPressed
        boolean isTabKeyPressed = evt.getKeyCode() == KeyEvent.VK_TAB;

        // Al apretar la tecla tab para indentar el codigo se agregan 4 espacios en el texto
        if (isTabKeyPressed) {
            // Evitar que el JTextPane maneje el Tab
            evt.consume();
            int caretPosition = jtpCode.getCaretPosition();
            try {
                // Insertar 4 espacios
                jtpCode.getDocument().insertString(caretPosition, "    ", null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }//GEN-LAST:event_jtpCodeKeyPressed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Compilador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                System.out.println("LookAndFeel no soportado: " + ex);
            }
            new Compilador().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbrir;
    private javax.swing.JButton btnCompilar;
    private javax.swing.JButton btnEjecutar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnGuardarComo;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JPanel buttonsFilePanel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextArea jtaOutputConsole;
    private javax.swing.JTextPane jtpCode;
    private javax.swing.JPanel panelButtonCompilerExecute;
    private javax.swing.JPanel rootPanel;
    private javax.swing.JTable tblTokens;
    // End of variables declaration//GEN-END:variables
}
