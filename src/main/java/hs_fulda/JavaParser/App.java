package hs_fulda.JavaParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.JsonPrinter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class App 
{
    public static void main( String[] args )
    {
		JSONObject config = parseConfigFile(args[0]);
		FileInputStream fileInputStream = getFileInputStream(args[1]);
		JSONObject astAsJson = parseJavaCode(fileInputStream);
    }

    private static JSONObject parseConfigFile(String configFilePath) {
		JSONParser parser = new JSONParser();	
		try {
			JSONObject config = (JSONObject) parser.parse(new FileReader(configFilePath));
    		System.out.println(config.get("methodToAnalyze"));
    		
    		JSONArray requiredConstructs = (JSONArray) config.get("requiredConstructs");
    		for (Object requiredConstruct : requiredConstructs) {
    			System.out.println(requiredConstruct);
    		}
    		return config;
        } catch(IndexOutOfBoundsException indexOutOfBoundsException) {
        	System.out.println("No config file was provided.");
        	return null;
 		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
    
	private static FileInputStream getFileInputStream(String javaCodeFilepath) {
		FileInputStream javaCodeFileStream = null;
		try {
			javaCodeFileStream = new FileInputStream(javaCodeFilepath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return javaCodeFileStream;
	}
	
	private static JSONObject parseJavaCode(FileInputStream javaCodeFileStream) {
		CompilationUnit compilationUnit = JavaParser.parse(javaCodeFileStream);
        JsonPrinter printer = new JsonPrinter(true);
        String ASTString = printer.output(compilationUnit.findRootNode());

        JSONParser jsonParser = new JSONParser();
        JSONObject ASTJson = new JSONObject();
        try {
        	ASTJson = (JSONObject) jsonParser.parse(ASTString);
        	System.out.println(ASTJson.get("types"));
        	return ASTJson;
        } catch(Exception exception) {
        	System.out.println(exception);
        }
        return null;
	}

}
