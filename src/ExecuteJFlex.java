/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


import java.util.logging.Level;
import java.util.logging.Logger;
import jflex.exceptions.SilentExit;

/**
 *
 * @author Yoshi Debat
 */
public class ExecuteJFlex {
    
    public static void main(String[] args) {
        String lexerFile = System.getProperty("user.dir")+"/src/Lexer.flex";
        
        try {
            jflex.Main.generate(new String[]{ lexerFile });
        } catch (SilentExit ex) {
            Logger.getLogger(ExecuteJFlex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
