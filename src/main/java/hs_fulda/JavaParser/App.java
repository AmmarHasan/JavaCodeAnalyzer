package hs_fulda.JavaParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class App 
{
    public static void main( String[] args )
    {
		JSONObject config = parseConfigFile(args[0]);
		FileInputStream fileInputStream = getFileInputStream(args[1]);
		boolean success = parseJavaCode(fileInputStream, config);
		System.out.println( "Test Program Success: "+success);
    }

    private static JSONObject parseConfigFile(String configFilePath) {
		JSONParser parser = new JSONParser();	
		try {
			JSONObject config = (JSONObject) parser.parse(new FileReader(configFilePath));
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

	private static boolean parseJavaCode (FileInputStream javaCodeFileStream, JSONObject config) {
		CompilationUnit cu = JavaParser.parse(javaCodeFileStream);
		String req_ClassName = config.get("classToAnalyze").toString();
		System.out.println("Required Class: " + req_ClassName );
		Stream<ClassOrInterfaceDeclaration> stream_Class = getFilteredClassStream (cu,req_ClassName);
		
		if (checkForClass(stream_Class)) {
			stream_Class = getFilteredClassStream (cu,req_ClassName);
			ClassOrInterfaceDeclaration classNode = stream_Class.findFirst().get();
			System.out.println("Found Class: "+ classNode.getNameAsString());
			
			String req_MethodName = config.get("methodToAnalyze").toString();
			System.out.println("Required Method: " + req_MethodName );
			Stream<MethodDeclaration> stream_Method = getFilteredMethodStream(classNode,req_MethodName);
	        
			if (checkForMethod(stream_Method)) {
	        	stream_Method = getFilteredMethodStream(classNode,req_MethodName);
	        	MethodDeclaration methodNode = stream_Method.findFirst().get();
	        	System.out.println("Found Method: "+ methodNode.getName());
	    		
	    		JSONArray requiredConstructs = (JSONArray) config.get("requiredConstructs");
	    		int successCounter = 0;
	    		for (Object requiredConstruct : requiredConstructs) {
	    			System.out.println("Required: "+requiredConstruct);
	    			if (requiredConstruct.equals("if")) {
	    				long sum = methodNode.findAll(IfStmt.class).stream()
	    				.count();
	            		System.out.println("Found: "+sum);
	            		if ( sum > 0) {
	            			System.out.println("If Statement found");
	            			successCounter++;
	            		} else {
	            			System.out.println("If Statement Not found");
//	            			return false;
	            		}
	    			} 
	    			else if (requiredConstruct.equals("for")) {
	    				long sum = methodNode.findAll(ForStmt.class).stream()
	        			.count();
	    				System.out.println("Found: "+sum);
	                    if ( sum > 0) {
	                    	System.out.println("For Statement found");
	                    	successCounter++;
	            		} else {
	            			System.out.println("For Statement Not found");
//	            			return false;
	            		}
	    			} 
	    			else {
	    				System.out.println("Finding "+ requiredConstruct + " not implemented.");
	    			}
	    		}
	    		if (successCounter == requiredConstructs.size()) {
	    			return true;
	    		} else {
					return false;
	    		}
	        } else {
				System.out.println("No Method found with the specified name.");
				return false;
			}
		} else {
			System.out.println("No Class found with the specified name.");
			return false;
		}
	}
	
	private static Stream<ClassOrInterfaceDeclaration> getFilteredClassStream ( CompilationUnit cu , String req_ClassName ) {
		return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
		        .filter(c -> c.getNameAsString().equals(req_ClassName))
		        .distinct();
	}
	
	private static Stream<MethodDeclaration> getFilteredMethodStream (ClassOrInterfaceDeclaration clas,String req_MethodName) {
		return clas.findAll(MethodDeclaration.class).stream()
            	.filter(method -> method.getNameAsString().equals(req_MethodName))
            	.distinct();
	}
	
	private static boolean checkForClass ( Stream<ClassOrInterfaceDeclaration> stream_Class ) {
		Stream<ClassOrInterfaceDeclaration> newStream = stream_Class;
		long count = newStream.count();
		if ( count > 0 ) {
//			System.out.println("Class Found: "+ count );
			return true;
		}
		return false;
	}
	
	private static boolean checkForMethod ( Stream<MethodDeclaration> stream_Method) {
		Stream<MethodDeclaration> newStream = stream_Method;
		long count = newStream.count();
		if ( count > 0 ) {
//			System.out.println("Method Found: "+ count );
			return true;
		}
		return false;
	}
	
	

}