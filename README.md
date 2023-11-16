# Sobre el Proyecto
El objetivo de este proyecto es desarrollar un compilador que transforme código en C a código Java, 
con el propósito de facilitar la migración de programas entre ambos lenguajes.

El alcance del proyecto, por los plazos de tiempo disponibles, se acotó para cubrir las siguientes
características del lenguaje C:
* Declaración y asignación de variables de tipo de datos int (ENTERO).
* Uso de funciones “printf” y “scanf”.
* Estructuras condicionales “if” y “else”.
* Estructura iterativa “for”.

Este programa provee una interfaz gráfica que permite realizar las siguientes acciones:
* Manejo y creación de archivos .c
* Escritura de código mediante un editor de código integrado.
* Visualización de tokens generados por el analizador léxico.

Las partes del proceso de compilación realizado son las siguientes:
1. **Analizador Léxico**: toma el código fuente ingresado y genera los tokens. Hecho con biblioteca JFlex.
2. **Analizador Sintáctico**: toma la secuencia de tokens generada y las agrupa en producciones significativas. Hecho con biblioteca Compiler Tools.
3. **Analizador Semántico**: validación de tipos de datos asignados a las variables.
4. **Traductor de Código**: transforma el código en C en un archivo .java.

# Ejecución del Proyecto
Para ejecutar el proyecto se requiere:
* Tener versión 12.0 o posterior de NetBeans.
* Tener la versión de JDK 11 o posterior para compatibilidad con las bibliotecas.

También es necesario importar, dentro del IDE, los archivos .jar de las bibliotecas utilizadas. Estos se encuentran 
en la raíz del proyecto, y son los siguientes:
* compilerTools-2.3.7.jar
* flatlaf-2.2.jar
* jflex-full-1.8.2.jar
