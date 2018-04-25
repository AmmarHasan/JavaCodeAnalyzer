package hs_fulda.JavaParser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.JsonPrinter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class App 
{
    public static void main( String[] args )
    {
        CompilationUnit compilationUnit = JavaParser.parse("class A {  public static void main( String[] args ) {"
        		+ "System.out.println(\"Hello World\");"
        		+ "} }");
        
        JsonPrinter printer = new JsonPrinter(true);
        String astString = printer.output(compilationUnit.findRootNode());
        
        JSONParser jsonParser = new JSONParser();
        try {
        	JSONObject astJSON = (JSONObject) jsonParser.parse(astString);
        	System.out.println(astJSON.get("types"));
        } catch(Exception exception) {
        	System.out.println(exception);
        }
        
    }
}
