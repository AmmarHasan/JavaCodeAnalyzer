package hs_fulda.JavaParser;

import java.awt.List;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.github.javaparser.printer.JsonPrinter;

public class Analyzer {
	public static void parseJavaFile(String javaFilePath, String configFilePath) {
		JSONObject config = Analyzer.parseConfigFile(configFilePath);
		FileInputStream fileInputStream = getFileInputStream(javaFilePath);
		CompilationUnit cu = JavaParser.parse(fileInputStream);
		printAST(cu);
		JavaCodeAnalyze(cu,config);
		
	}
	
	private static boolean JavaCodeAnalyze(CompilationUnit cu, JSONObject config) {
		String req_ClassName = config.get("name").toString();
		System.out.println("Required Class: " + req_ClassName);
		
		
		Stream<ClassOrInterfaceDeclaration> stream_Class = getFilteredClassStream(cu, req_ClassName);

		if (checkForClass(stream_Class)) {
			stream_Class = getFilteredClassStream(cu, req_ClassName);
			ClassOrInterfaceDeclaration classNode = stream_Class.findFirst().get();
			System.out.println("Found Class: " + classNode.getNameAsString());
			
			//Check Class Access Modifier		
			JSONArray requiredConstructs = (JSONArray) config.get("accessModifier");
			for (Object requiredConstruct : requiredConstructs) {
				JSONObject req_Construct = (JSONObject) requiredConstruct;
				String req_ConstructName = req_Construct.get("name").toString();
				String req_ConstructRule = req_Construct.get("forbidden").toString();			
				String compare = classNode.getModifiers().toString().toLowerCase();
				
				//Checking if Modifier is Allowed or Forbidden
				if(compare.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("false")) {
					System.out.println("Required Access Modifier: " + req_ConstructName);
					System.out.println("Found Required Access Modifier: "+ req_ConstructName);
					
				} else if(compare.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("true")) {
					System.out.println("Found Forbidden Access Modifier: "+req_ConstructName);
				} 
			}
			
/*			//Checking Class Modifier
			JSONArray neededConstructs = (JSONArray) config.get("modifier");
			for (Object neededConstruct : neededConstructs) {
				JSONObject req_Construct = (JSONObject) neededConstruct;
				String req_ConstructName = req_Construct.get("name").toString();
				String req_ConstructRule = req_Construct.get("forbidden").toString();
				
				String compare = classNode.getModifiers().toString().toLowerCase();
				
				if(compare.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("false")) {
					System.out.println("Required Modifier: " + req_ConstructName);
					System.out.println("Found Required Modifier: "+req_ConstructName);
					
				} else if(compare.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("true")) {
					System.out.println("Found Forbidden Modifier: "+req_ConstructName);
				} 
	 
			}*/
			
			//Checking for Super Class
			JSONArray req_SuperClass = (JSONArray) config.get("parentClass");
			for (Object requiredConstruct : req_SuperClass) {
				JSONObject req_Construct = (JSONObject) requiredConstruct;
				String req_ConstructName = req_Construct.get("name").toString();
				String req_ConstructRule = req_Construct.get("forbidden").toString();
				String compSuper = classNode.getExtendedTypes().toString().toUpperCase();
				
				//Checking SuperClass
				if(compSuper.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("false")) {
					System.out.println("Required Parent Class: " + req_ConstructName);
					System.out.println("Found Required Parent Class: "+req_ConstructName);
					
				} else if(compSuper.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("true")) {
					System.out.println("Found Forbidden Parent Class: "+ req_ConstructName);
				} 
				
			}
			//Checking for Interface
			JSONArray req_Interfaces = (JSONArray) config.get("interfaceToImplement");
			for (Object requiredConstruct : req_Interfaces) {
				JSONObject req_Construct = (JSONObject) requiredConstruct;
				String req_ConstructName = req_Construct.get("name").toString();
				String req_ConstructRule = req_Construct.get("forbidden").toString();
				String compare = classNode.getImplementedTypes().toString().toUpperCase();
				
				//Checking Interface
				if(compare.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("false")) {
					System.out.println("Required Interface: " + req_ConstructName);
					System.out.println("Found Required Interface: "+req_ConstructName);
					
				} else if(compare.contains((CharSequence) req_ConstructName) && req_ConstructRule.equalsIgnoreCase("true")) {
					System.out.println("Found Forbidden Interface: "+ req_ConstructName);
				} 
				
			}

		}
		return false;	
	}

	public static void parseJavaCode(String javaCode, String configFilePath) {
		JSONObject config = Analyzer.parseConfigFile(configFilePath);
		CompilationUnit cu = JavaParser.parse(javaCode);
		boolean success = JavaCodeAnalyze(cu, config);
		System.out.println("Test Program Success: " + success);
	}

	private static JSONObject parseConfigFile(String configFilePath) {
		JSONParser parser = new JSONParser();
		try {
			JSONObject config = (JSONObject) parser.parse(new FileReader(configFilePath));
			System.out.println(config.get("requiredClass").toString());
			return (JSONObject) config.get("requiredClass");
		} catch (IndexOutOfBoundsException indexOutOfBoundsException) {
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

	private static Stream<ClassOrInterfaceDeclaration> getFilteredClassStream ( CompilationUnit cu , String req_ClassName ) {
		return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
		        .filter(c -> c.getNameAsString().equals(req_ClassName))
		        .distinct();
	}

	private static boolean checkForClass(Stream<ClassOrInterfaceDeclaration> stream_Class) {
		Stream<ClassOrInterfaceDeclaration> newStream = stream_Class;
		long count = newStream.count();
		if (count > 0) {
			// System.out.println("Class Found: "+ count );
			return true;
		}
		return false;
	}

	private static void printAST(CompilationUnit compilationUnit) {
		JsonPrinter printer = new JsonPrinter(true);
		String astString = printer.output(compilationUnit.findRootNode());

		JSONParser jsonParser = new JSONParser();
		try {
			JSONObject astJSON = (JSONObject) jsonParser.parse(astString);
			System.out.println(astJSON.get("types"));
		} catch (Exception exception) {
			System.out.println(exception);
		}

	}

}