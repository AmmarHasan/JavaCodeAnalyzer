package hs_fulda.JavaParser;

import java.awt.List;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
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
			System.out.println(config.get("requiredClass").toString());
    		return (JSONObject) config.get("requiredClass");
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
		String req_ClassName = config.get("name").toString();
		String req_AccesModifier = config.get("requiredAccessModifier").toString();
		String req_Parent = config.get("requiredParentClass").toString();
		
		System.out.println("Required Class: " + req_ClassName );
		System.out.println("Required Acces Modifier: " + req_AccesModifier );
		System.out.println("Required Parent Modifier: " + req_Parent );
		
		JSONArray forbdn_AccesModifiers = (JSONArray) config.get("forbiddenAccessModifier");
		ArrayList<String> f_accessMods = new ArrayList<String>();
		for (Object forbdn_AccesModifier : forbdn_AccesModifiers) {
			f_accessMods.add(forbdn_AccesModifier.toString());
			System.out.println("Forbidden Access Modifiers: " + forbdn_AccesModifier );
		}
		JSONArray reqd_Modifiers = (JSONArray) config.get("requiredModifiers");
		ArrayList<String> r_Mods = new ArrayList<String>();
		for (Object reqd_Modifier : reqd_Modifiers) {
			r_Mods.add(reqd_Modifier.toString());
			System.out.println("Required Modifiers: " + reqd_Modifier );
		}
		JSONArray forbdn_Modifiers = (JSONArray) config.get("forbiddenModifier");
		ArrayList<String> f_Mods = new ArrayList<String>();
		for (Object forbdn_Modifier : forbdn_Modifiers) {
			f_Mods.add(forbdn_Modifier.toString());
			System.out.println("Forbidden Modifiers: " + forbdn_Modifier );
		}
		
		Stream<ClassOrInterfaceDeclaration> stream_Class = getFilteredClassStream (cu,req_ClassName,req_AccesModifier,f_accessMods,r_Mods,f_Mods,req_Parent);
		
		if (checkForClass(stream_Class)) {
			stream_Class = getFilteredClassStream (cu,req_ClassName,req_AccesModifier,f_accessMods,r_Mods,f_Mods,req_Parent);
			ClassOrInterfaceDeclaration classNode = stream_Class.findFirst().get();
			System.out.println("Found Class: "+ classNode.getNameAsString());

//			return true;
			JSONObject methodRequirements = (JSONObject) config.get("requiredMethod");
			String req_MethodName = methodRequirements.get("name").toString();
			System.out.println("Required Method: " + req_MethodName );
			Stream<MethodDeclaration> stream_Method = getFilteredMethodStream(classNode,req_MethodName);
	        
			if (checkForMethod(stream_Method)) {
	        	stream_Method = getFilteredMethodStream(classNode,req_MethodName);
	        	MethodDeclaration methodNode = stream_Method.findFirst().get();
	        	System.out.println("Found Method: "+ methodNode.getName());
	    		
	    		JSONArray requiredConstructs = (JSONArray) methodRequirements.get("requiredConstructs");
	    		int successCounter = 0;
	    		
	    	
	    		for (Object requiredConstruct : requiredConstructs) {
	    			JSONObject req_Construct = (JSONObject) requiredConstruct;
	    			String req_ConstructName = req_Construct.get("name").toString();
	    			System.out.println("Required: "+req_ConstructName);
	    			if (req_ConstructName.equals("if")) {
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
	    			else if (req_ConstructName.equals("for")) {
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
			System.out.println("No Class found with the specified properties.");
			return false;
		}
	}
	
	private static Stream<ClassOrInterfaceDeclaration> getFilteredClassStream(CompilationUnit cu,String req_ClassName,String accessMod,
			ArrayList<String> f_accessMods,ArrayList<String> rMods,ArrayList<String> fMods,String req_Parent) {
		
		return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
		        .filter( c -> c.getNameAsString().equals(req_ClassName)
		        		// Required Access Modifier
		        		 && ( accessMod.equals("public")  ? c.isPublic() : true )
		        		 && ( accessMod.equals("private")  ? c.isPrivate() : true )
		        		 && ( accessMod.equals("protected")  ? c.isProtected() : true )
		        		// Forbidden Access Modifier
		        		 && ( f_accessMods.contains("public")  ? !c.isPublic() : true )
		        		 && ( f_accessMods.contains("protected")  ? !c.isProtected() : true )
		        		 && ( f_accessMods.contains("private")  ? !c.isPrivate() : true )
		        		// Required Modifier
		        		 && ( rMods.contains("abstract")  ? c.isAbstract() : true )
		        		 && ( rMods.contains("final")  ? c.isFinal() : true )
		        		 && ( rMods.contains("static")  ? c.isStatic(): true )
		        		 && ( rMods.contains("interface")  ? c.isInterface(): true )
		        		// Forbidden Modifier
		        		 && ( fMods.contains("abstract")  ? !c.isAbstract() : true )
		        		 && ( fMods.contains("final")  ? !c.isFinal() : true )
		        		 && ( fMods.contains("static")  ? !c.isStatic(): true )
		        		 && ( rMods.contains("interface")  ? !c.isInterface(): true )
		        		)
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