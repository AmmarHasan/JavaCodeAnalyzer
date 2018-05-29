package hs_fulda.JavaParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.EnumSet;
import java.util.Optional;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
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
     		MethodDeclaration methodDeclaration = checkMethodSignature(cu,requiredMethod);
     		if ( methodDeclaration != null ) {
     			// test further
     			JSONArray requiredAMods = (JSONArray) requiredMethod.get("accessModifiers");
     			int counter = 0;
     			for(int j = 0; j < requiredAMods.size() ; j++){
     	     		JSONObject requiredAMod = (JSONObject) requiredAMods.get(j);
     	     		if (checkMethodAccessModifier (methodDeclaration, requiredAMod)) {
     	     			counter ++;
     	     		}
     	     	}
     			if (counter == requiredAMods.size() ){
     				JSONArray requiredMods = (JSONArray) requiredMethod.get("modifiers");
     				counter = 0;
     				for(int j = 0 ; j < requiredMods.size() ; j++){
         	     		JSONObject requiredMod = (JSONObject) requiredMods.get(j);
         	     		if (checkMethodOtherModifier (methodDeclaration, requiredMod)) {
         	     			counter ++;
         	     		}
         	     	}
     				if (counter == requiredMods.size() ){
         				JSONArray annotations = (JSONArray) requiredMethod.get("annotations");
         				counter = 0;
         				for(int j = 0 ; j < annotations.size() ; j++){
             	     		JSONObject annotation = (JSONObject) annotations.get(j);
             	     		if (checkMethodAnnotation (methodDeclaration, annotation)) {
             	     			counter ++;
             	     		}
             	     	}
         				//
         			}
     			}
     			
     			
     		}
     	 }
   }
	
	private static MethodDeclaration checkMethodSignature (CompilationUnit cu, JSONObject requiredMethod) {
		MethodDeclaration methodDeclaration = checkMethodName ( cu, requiredMethod.get("name").toString() );
		if (methodDeclaration != null) {
 			if (checkMethodReturnType ( methodDeclaration, requiredMethod.get("return").toString())) {
 				if (checkMethodParameters ( methodDeclaration, (JSONArray) requiredMethod.get("parameters") )) {
 					return methodDeclaration;
 				}else{
 					return null;
 				}
 			}else{
				return null;
			}
 		}else{
			return null;
 		}
	}
   
	private static MethodDeclaration checkMethodName (CompilationUnit cu, String requiredName) {
  	 
		JSONObject required = new JSONObject ();
		required.put( "name", requiredName );
		VoidVisitor<JSONObject> methodNameTester = new MethodNameTester ();
		methodNameTester.visit(cu, required); 
		displayResult(required);
		
		try {
			MethodDeclaration Md = (MethodDeclaration) required.get("md");
			return Md;
		} catch (Exception exception) {
			return null;
		}
	}
   
	private static Boolean checkMethodReturnType (MethodDeclaration md, String requiredReturnType) {
     	 
		JSONObject required = new JSONObject ();
		required.put("returnType", requiredReturnType );
		VoidVisitor<JSONObject> methodReturnTypeTester = new MethodReturnTypeTester ();
		methodReturnTypeTester.visit(md, required); 
		displayResult(required);
		return (Boolean) required.get("success");
   }
   
	private static Boolean checkMethodParameters (MethodDeclaration md, JSONArray requiredParametersArray) {
   	 
		JSONObject requiredParameters = new JSONObject ();
		requiredParameters.put("parameters", requiredParametersArray );
		VoidVisitor<JSONObject> methodParametersTester = new MethodParametersTester ();
		methodParametersTester.visit(md, requiredParameters); 
		displayResult(requiredParameters);
		return (Boolean) requiredParameters.get("success");
	}
	
	private static Boolean checkMethodAccessModifier (MethodDeclaration md, JSONObject requiredAccessModifier ) {
	   	 
	   	JSONObject required = new JSONObject ();
	   	String requiredModifierName = requiredAccessModifier.get("name").toString();
	   	Boolean requirement = true;
	   	if(requiredAccessModifier.containsKey("forbidden") ){
	   		if ( (Boolean) requiredAccessModifier.get("forbidden") ) {
	   			requirement = false;
	   		}
	   	}
	   	required.put("requiredModifierName", requiredModifierName );
	   	required.put("requirement", requirement);
	   	VoidVisitor<JSONObject> methodAccessModifierTester = new MethodAccessModifierTester ();
	   	methodAccessModifierTester.visit(md, required); 
	   	displayResult(required);
	   	return (Boolean) required.get("success");
	}
   
	private static Boolean checkMethodOtherModifier (MethodDeclaration md, JSONObject requiredModifier ) {
	   	 
	   	JSONObject required = new JSONObject ();
	   	String requiredModifierName = requiredModifier.get("name").toString();
	   	Boolean requirement = true;
	   	if(requiredModifier.containsKey("forbidden") ){
	   		if ( (Boolean) requiredModifier.get("forbidden") ) {
	   			requirement = false;
	   		}
	   	}
	   	required.put("requiredModifierName", requiredModifierName );
	   	required.put("requirement", requirement);
	   	VoidVisitor<JSONObject> methodOtherModifierTester = new MethodOtherModifierTester ();
	   	methodOtherModifierTester.visit(md, required); 
	   	displayResult(required);
	   	return (Boolean) required.get("success");
	}
	
	private static Boolean checkMethodAnnotation (MethodDeclaration md, JSONObject requiredAnnotation ) {
	   	 
	   	JSONObject required = new JSONObject ();
	   	String requiredAnnotationName = requiredAnnotation.get("name").toString();
	   	Boolean requirement = true;
	   	if(requiredAnnotation.containsKey("forbidden") ){
	   		if ( (Boolean) requiredAnnotation.get("forbidden") ) {
	   			requirement = false;
	   		}
	   	}
	   	required.put("requiredAnnotationName", requiredAnnotationName );
	   	required.put("requirement", requirement);
	   	VoidVisitor<JSONObject> methodAnnotationsTester = new MethodAnnotationsTester ();
	   	methodAnnotationsTester.visit(md, required); 
	   	displayResult(required);
	   	return (Boolean) required.get("success");
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
              	jobject.put("md", md );
           }
           else {
           	jobject.put("success", false);
           	jobject.put("errorCode", 210);
           	jobject.put("range", md.getRange().get());
           	jobject.put("md", null );
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
           	jobject.put("errorCode", 220);
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

   private static class MethodAccessModifierTester extends VoidVisitorAdapter<JSONObject> {

       @Override
       public void visit(MethodDeclaration md, JSONObject jobject) { 
           super.visit(md, jobject);
           if ( jobject.containsKey("success") && (Boolean) jobject.get("success")) {
        	   return;
           }
           
           EnumSet<Modifier> modifiers = md.getModifiers();
           AccessSpecifier accessSpecifier = Modifier.getAccessSpecifier(modifiers);
           String accessSpecifierString = accessSpecifier.asString();
           Boolean requirement = (Boolean) jobject.get("requirement");
           String reqdAccessSpecifier = jobject.get("requiredModifierName").toString();
           
           if ( accessSpecifierString.equals(reqdAccessSpecifier) == requirement) {
           		jobject.put("success", true);
              	jobject.put("errorCode", 0);
              	jobject.put("range", md.getRange().get());
           }
           else {
           	jobject.put("success", false);
           	jobject.put("errorCode", 240);
           	jobject.put("range", md.getRange().get());
           	jobject.put("cu", Optional.empty() );
           }
       }
   }
   
   private static class MethodOtherModifierTester extends VoidVisitorAdapter<JSONObject> {

       @Override
       public void visit(MethodDeclaration md, JSONObject jobject) { 
           super.visit(md, jobject);
           if ( jobject.containsKey("success") && (Boolean) jobject.get("success")) {
        	   return;
           }
           
           EnumSet<Modifier> modifiers = md.getModifiers();
           Boolean requirement = (Boolean) jobject.get("requirement");
           String reqdSpecifier = jobject.get("requiredModifierName").toString();
           Boolean modifierFound = false;
           for ( Modifier modifier : modifiers ) {
        	   if ( modifier.asString().equalsIgnoreCase(reqdSpecifier) ) {
        		   modifierFound = true;
        		   break;
        	   }
           }
           
           if ( modifierFound == requirement) {
           		jobject.put("success", true);
              	jobject.put("errorCode", 0);
              	jobject.put("range", md.getRange().get());
           }
           else {
           		jobject.put("success", false);
           		jobject.put("errorCode", 250);
           		jobject.put("range", md.getRange().get());
           		jobject.put("cu", Optional.empty() );
           }
       }
   }
   
   private static class MethodAnnotationsTester extends VoidVisitorAdapter<JSONObject> {

       @Override
       public void visit(MethodDeclaration md, JSONObject jobject) { 
           super.visit(md, jobject);
           if ( jobject.containsKey("success") && (Boolean) jobject.get("success")) {
        	   return;
           }
           
           NodeList<AnnotationExpr> annotationExprs = md.getAnnotations();

           Boolean requirement = (Boolean) jobject.get("requirement");
           String reqdAnnotation = jobject.get("requiredAnnotationName").toString();
           Boolean annotationFound = false;
           
           for ( AnnotationExpr annotationExpr : annotationExprs ) {
        	   if ( annotationExpr.getNameAsString().equalsIgnoreCase(reqdAnnotation) ) {
        		   annotationFound = true;
        		   break;
        	   }
           }
           
           if ( annotationFound == requirement) {
           		jobject.put("success", true);
              	jobject.put("errorCode", 0);
              	jobject.put("range", md.getRange().get());
           }
           else {
           		jobject.put("success", false);
           		jobject.put("errorCode", 260);
           		jobject.put("range", md.getRange().get());
           		jobject.put("cu", Optional.empty() );
           }
       }
   }
   
//   private static void putResult ( JSONObject result) {}
  
   
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