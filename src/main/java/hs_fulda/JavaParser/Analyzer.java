package hs_fulda.JavaParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Optional;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.github.javaparser.printer.JsonPrinter;

public class Analyzer {
	public static void parseJavaFile(String javaFilePath, String configFilePath) {
		JSONObject config = Analyzer.parseConfigFile(configFilePath);
		FileInputStream fileInputStream = getFileInputStream(javaFilePath);
		CompilationUnit cu = JavaParser.parse(fileInputStream);
		
		checkMethods ( cu ,config);
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

	// New Parsing Methods
	
	private static void checkMethods (CompilationUnit cu, JSONObject config) {
     	 
     	 JSONArray requiredMethods = (JSONArray) config.get("requiredMethods");
     	 for(int i = 0 ; i < requiredMethods.size() ; i++)
     	 {
     		JSONObject requiredMethod = (JSONObject) requiredMethods.get(i);
     		if ( checkMethodSignature(cu,requiredMethod) ) {
     			// test further
     		}
     	 }
   }
	
	private static Boolean checkMethodSignature (CompilationUnit cu, JSONObject requiredMethod) {
		Optional<CompilationUnit> classCu = checkMethodName ( cu, requiredMethod.get("name").toString() );
 		if (classCu.isPresent()) {
 			if (checkMethodReturnType ( classCu.get(), requiredMethod.get("return").toString())) {
 				if (checkMethodParameters ( classCu.get(), (JSONArray) requiredMethod.get("parameters") )) {
 					return true;
 				}else{
 					return false;
 				}
 			}else{
				return false;
			}
 		}else{
			return false;
 		}
	}
   
	private static Optional<CompilationUnit> checkMethodName (CompilationUnit cu, String requiredName) {
  	 
		JSONObject required = new JSONObject ();
		required.put("name", requiredName );
		VoidVisitor<JSONObject> methodNameTester = new MethodNameTester ();
		methodNameTester.visit(cu, required); 
		displayResult(required);
		Optional<CompilationUnit> optionalCu = (Optional<CompilationUnit>) required.get("cu");
		return optionalCu;
   }
   
	private static Boolean checkMethodReturnType (CompilationUnit cu, String requiredReturnType) {
     	 
		JSONObject required = new JSONObject ();
		required.put("returnType", requiredReturnType );
		VoidVisitor<JSONObject> methodReturnTypeTester = new MethodReturnTypeTester ();
		methodReturnTypeTester.visit(cu, required); 
		displayResult(required);
		return (Boolean) required.get("success");
   }
   
	private static Boolean checkMethodParameters (CompilationUnit cu, JSONArray requiredParametersArray) {
   	 
   	JSONObject requiredParameters = new JSONObject ();
   	requiredParameters.put("parameters", requiredParametersArray );
   	VoidVisitor<JSONObject> methodParametersTester = new MethodParametersTester ();
   	methodParametersTester.visit(cu, requiredParameters); 
   	displayResult(requiredParameters);
   	return (Boolean) requiredParameters.get("success");
   }
   
	private static void displayResult ( JSONObject result) {
		if ( !(Boolean) result.get("success") ) {
			System.out.println ("Error: " + result.get("errorCode"));
		    System.out.println ("Location: " + ((Range) result.get("range")).begin);
		}
		else {
			System.out.println ("OK !");
		}
	    
	}
	
   // Overriding Visit Methods
   
   private static class MethodNameTester extends VoidVisitorAdapter<JSONObject> {

       @Override
       public void visit(MethodDeclaration md, JSONObject jobject) { 
           super.visit(md, jobject);
           if ( jobject.containsKey("success") && (Boolean) jobject.get("success")) {
           	return;
           }
           
           String name = md.getNameAsString();
           if ( name.equals(jobject.get("name").toString())) {
           		jobject.put("success", true);
              	jobject.put("errorCode", 0);
              	jobject.put("range", md.getRange().get());
              	jobject.put("cu", md.findCompilationUnit() );
           }
           else {
           	jobject.put("success", false);
           	jobject.put("errorCode", 210);
           	jobject.put("range", md.getRange().get());
           	jobject.put("cu", Optional.empty() );
           }
       }
   }
   
   private static class MethodReturnTypeTester extends VoidVisitorAdapter<JSONObject> {

       @Override
       public void visit(MethodDeclaration md, JSONObject jobject) { 
           super.visit(md, jobject);
           if ( jobject.containsKey("success") && (Boolean) jobject.get("success")) {
           	return;
           }
           
           String returnType = md.getTypeAsString();
           
           if ( returnType.equals(jobject.get("returnType").toString())) {
           	jobject.put("success", true);
              	jobject.put("errorCode", 0);
               jobject.put("range", md.getRange().get());
               jobject.put("cu", md.findCompilationUnit() );
           }
           else {
           	jobject.put("success", false);
           	jobject.put("errorCode", 210);
           	jobject.put("range", md.getRange().get());
           }
       }
   }
   
   private static class MethodParametersTester extends VoidVisitorAdapter<JSONObject> {

       @Override
       public void visit(MethodDeclaration md, JSONObject jobject) { //ArrayList<String> requiredParameters
           super.visit(md, jobject);
           if ( jobject.containsKey("success") && (Boolean) jobject.get("success")) {
           	return;
           }
           
           NodeList<com.github.javaparser.ast.body.Parameter> codeParameters = md.getParameters() ;
           JSONArray requiredParametersJSONArray = (JSONArray) jobject.get("parameters");
           if ( requiredParametersJSONArray.size() == codeParameters.size() ) {
           	int matchCounter = 0 ;
           	for(int i = 0 ; i < requiredParametersJSONArray.size() ; i++){
           		if (requiredParametersJSONArray.get(i).toString().equalsIgnoreCase(codeParameters.get(i).getTypeAsString())) {
           			matchCounter ++;
           		}
           	}
           	if (matchCounter == codeParameters.size()) {
           		jobject.put("success", true);
               	jobject.put("errorCode", 0);
               	jobject.put("range", md.getRange().get());
           	} else {
               	jobject.put("success", false);
               	jobject.put("errorCode", 231);
               	jobject.put("range", md.getRange().get());
               }
           	
           }
           else {
           	jobject.put("success", false);
           	jobject.put("errorCode", 230);
           	jobject.put("range", md.getRange().get());
           }
       }
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