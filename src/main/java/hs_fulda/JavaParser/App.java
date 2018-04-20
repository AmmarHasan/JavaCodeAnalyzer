package hs_fulda.JavaParser;
import java.util.Optional;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class App 
{
    public static void main( String[] args )
    {
        CompilationUnit compilationUnit = JavaParser.parse("class A {  public static void main( String[] args ) {} }");
        Optional<ClassOrInterfaceDeclaration> classA = compilationUnit.getClassByName("A");
       
        System.out.println(classA);

    }
}
