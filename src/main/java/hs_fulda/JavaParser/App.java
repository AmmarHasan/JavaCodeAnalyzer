package hs_fulda.JavaParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class App 
{
    public static void main( String[] args )
    {
		JSONObject config = parseConfigFile(args[0]);
		FileInputStream fileInputStream = getFileInputStream(args[1]);
		parseJavaCode(fileInputStream, config);
    }

    private static JSONObject parseConfigFile(String configFilePath) {
		JSONParser parser = new JSONParser();	
		try {
			JSONObject config = (JSONObject) parser.parse(new FileReader(configFilePath));
    		
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

	private static void parseJavaCode (FileInputStream javaCodeFileStream, JSONObject config) {
		CompilationUnit cu = JavaParser.parse(javaCodeFileStream);
		cu.findAll(ClassOrInterfaceDeclaration.class).stream()
        .filter(c -> c.getNameAsString().equals( config.get("classToAnalyze"))
        			&& !c.isInterface() && !c.isAbstract() )
        .forEach(c -> {
            c.findAll(MethodDeclaration.class).stream()
            	.filter(method -> method.getNameAsString().equals(config.get("methodToAnalyze")))
            	.forEach(method -> {
            		System.out.println(method);
            	});
        });
	}

}