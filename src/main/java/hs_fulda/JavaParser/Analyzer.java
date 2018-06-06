package hs_fulda.JavaParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Range;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.JsonPrinter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Analyzer {
    public static void parseJavaFile(String javaFilePath, String configFilePath) {
        JSONObject config = Analyzer.parseConfigFile(configFilePath);
        FileInputStream fileInputStream = getFileInputStream(javaFilePath);
        CompilationUnit cu = JavaParser.parse(fileInputStream);
        printAST(cu);
        analyzeClass(cu, config);
        checkMethods(cu, config);
        // boolean success = JavaCodeAnalyze(cu, config);
        // System.out.println("Test Program Success: " + success);

    }

    @SuppressWarnings("unchecked")
    private static ClassOrInterfaceDeclaration analyzeClass(CompilationUnit cu, JSONObject config) {
        String req_ClassName = config.get("name").toString();
        // System.out.println("Required Class: " + req_ClassName);
        int status = 0;
        List<Integer> currentStatus = new ArrayList<Integer>();
        Stream<ClassOrInterfaceDeclaration> stream_Class = getFilteredClassStream(cu, req_ClassName);

        if (checkForClass(stream_Class)) {
            stream_Class = getFilteredClassStream(cu, req_ClassName);
            ClassOrInterfaceDeclaration classNode = stream_Class.findFirst().get();
            // System.out.println("Found Class: " + classNode.getNameAsString());

            // Check Class Access Modifier
            JSONArray requiredConstructs = (JSONArray) config.get("accessModifier");
            for (Object requiredConstruct : requiredConstructs) {
                JSONObject req_Construct = (JSONObject) requiredConstruct;
                String req_ConstructName = req_Construct.get("name").toString();
                String req_ConstructRule = req_Construct.getOrDefault("forbidden", false).toString();
                String compare = classNode.getModifiers().toString().toLowerCase();

                // Checking if Modifier is Allowed or Forbidden
                if (isContain(compare, req_ConstructName) == true && req_ConstructRule.equalsIgnoreCase("false")) {

                    status = 1;
                } else if (isContain(compare, req_ConstructName) == true
                        && req_ConstructRule.equalsIgnoreCase("true")) {
                    System.out.println("Found Forbidden Access Modifier: " + req_ConstructName);
                    status = 2;
                } else if (isContain(compare, req_ConstructName) == false
                        && req_ConstructRule.equalsIgnoreCase("false")) {
                    System.out.println("Required Access Modifier not Found: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;

                }
                currentStatus.add(status);
            }

            // Checking Class Modifier
            JSONArray neededConstructs = (JSONArray) config.get("modifier");
            for (Object neededConstruct : neededConstructs) {
                JSONObject req_Construct = (JSONObject) neededConstruct;
                String req_ConstructName = req_Construct.get("name").toString();
                String req_ConstructRule = req_Construct.getOrDefault("forbidden", false).toString();

                String compare = classNode.getModifiers().toString().toLowerCase();

                if (isContain(compare, req_ConstructName) == true && req_ConstructRule.equalsIgnoreCase("false")) {
                    // System.out.println("Required Modifier: " + req_ConstructName);
                    // System.out.println("Found Required Modifier: "+req_ConstructName);
                    status = 1;

                } else if (isContain(compare, req_ConstructName) == true
                        && req_ConstructRule.equalsIgnoreCase("true")) {
                    System.out.println("Found Forbidden Modifier: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;
                } else if (isContain(compare, req_ConstructName) == false
                        && req_ConstructRule.equalsIgnoreCase("false")) {
                    System.out.println("Required Modifier not Found: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;

                }
                currentStatus.add(status);
            }

            // Checking for Super Class
            JSONArray req_SuperClass = (JSONArray) config.get("parentClass");
            for (Object requiredConstruct : req_SuperClass) {
                JSONObject req_Construct = (JSONObject) requiredConstruct;
                String req_ConstructName = req_Construct.get("name").toString();

                String req_ConstructRule = req_Construct.getOrDefault("forbidden", false).toString();
                // String req_ConstructRule = req_Construct.get("forbidden").toString();
                String compSuper = classNode.getExtendedTypes().toString();

                if (isContain(compSuper, req_ConstructName) == true && req_ConstructRule.equalsIgnoreCase("false")) {
                    // System.out.println("Required Parent Class: " + req_ConstructName);
                    // System.out.println("Found Required Parent Class: "+req_ConstructName);
                    // break;
                    status = 1;
                } else if (isContain(compSuper, req_ConstructName) == true
                        && req_ConstructRule.equalsIgnoreCase("true")) {
                    System.out.println("Found Forbidden Parent Class: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;
                    break;
                } else if (classNode.getExtendedTypes().isEmpty()) {
                    System.out.println("No Parent Class Extended");
                    printLine(classNode);
                    status = 2;
                    break;
                } else if (isContain(compSuper, req_ConstructName) == false
                        && req_ConstructRule.equalsIgnoreCase("false")) {
                    System.out.println("Required Parent Class not Extended: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;

                }
                // else if(isContain(compSuper, req_ConstructName) == false &&
                // req_ConstructRule.equalsIgnoreCase("false")) {
                // System.out.println("Extended an unspecified Parent Class");
                // printLine(classNode);
                // status = 2;
                // break;
                // }
                currentStatus.add(status);
            }

            // Checking for Interface
            JSONArray req_Interfaces = (JSONArray) config.get("interfaceToImplement");
            for (Object requiredConstruct : req_Interfaces) {
                JSONObject req_Construct = (JSONObject) requiredConstruct;
                String req_ConstructName = req_Construct.get("name").toString();
                String req_ConstructRule = req_Construct.getOrDefault("forbidden", false).toString();
                String compare = classNode.getImplementedTypes().toString();

                // Checking Interface
                if (isContain(compare, req_ConstructName) == true && req_ConstructRule.equalsIgnoreCase("false")) {
                    // System.out.println("Required Interface: " + req_ConstructName);
                    // System.out.println("Found Required Interface: "+req_ConstructName);
                    status = 1;
                } else if (isContain(compare, req_ConstructName) == true
                        && req_ConstructRule.equalsIgnoreCase("true")) {
                    System.out.println("Found Forbidden Interface: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;
                } else if (classNode.getImplementedTypes().isEmpty()) {
                    System.out.println("No Interface Implemented");
                    printLine(classNode);
                    status = 2;
                    break;
                } else if (isContain(compare, req_ConstructName) == false
                        && req_ConstructRule.equalsIgnoreCase("false")) {
                    System.out.println("Required Interface not Implemented: " + req_ConstructName);
                    printLine(classNode);
                    status = 2;
                }
                // else if(isContain(compare, req_ConstructName) == false &&
                // req_ConstructRule.equalsIgnoreCase("false")) {
                // System.out.println("Implemented an unspecified Interface");
                // printLine(classNode);
                // status = 1;
                // break;
                // }
                currentStatus.add(status);

            }

            if (currentStatus.contains(2)) {
                System.out.println("Test Status: Failed");
                return null;
            } else {
                System.out.println("Test Status: Successful");
                return classNode;
            }

        } else {
            System.out.println("The required class was not found");
            return null;
        }
    }

    private static Stream<ClassOrInterfaceDeclaration> getFilteredClassStream(CompilationUnit cu,
            String req_ClassName) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(c -> c.getNameAsString().equals(req_ClassName)).distinct();
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

    private static boolean isContain(String source, String subItem) {
        String pattern = "\\b" + subItem + "\\b";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(source);
        return m.find();
    }

    private static void printLine(Node node) {
        node.getRange().ifPresent(r -> System.out.println("line: " + r.begin.line));
    }

    public static void parseJavaCode(String javaCode, String configFilePath) {
        JSONObject config = Analyzer.parseConfigFile(configFilePath);
        CompilationUnit cu = JavaParser.parse(javaCode);
        ClassOrInterfaceDeclaration classDeclaration = analyzeClass(cu, config);
        if (classDeclaration != null) {
            // Check Method
        }
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

    private static void checkMethods(CompilationUnit cu, JSONObject config) {

        JSONArray requiredMethodsConfig = (JSONArray) config.get("requiredMethods");
        for (int i = 0; requiredMethodsConfig != null && i < requiredMethodsConfig.size(); i++) {
            // Each object inside `requiredMethods` array
            JSONObject methodConfig = (JSONObject) requiredMethodsConfig.get(i);
            MethodDeclaration methodDeclaration = checkMethodSignature(cu, methodConfig);
            if (methodDeclaration != null) {
                checkMethodAccessModifier(methodDeclaration, methodConfig);
                checkMethodOtherModifier(methodDeclaration, methodConfig);
                checkMethodAnnotation(methodDeclaration, methodConfig);
                int counter = 0;
                if ((Boolean) methodConfig.get("restrictUserDefinedMethod")) {
                    Optional<BlockStmt> codeBlock = methodDeclaration.getBody();
                    if (codeBlock.isPresent()) {
                        if (checkUserDefinedMethodCall(codeBlock.get())) {
                            JSONArray builtInMethods = (JSONArray) methodConfig.get("builtInMethods");
                            counter = 0;
                            for (int j = 0; j < builtInMethods.size(); j++) {
                                JSONObject builtInMethod = (JSONObject) builtInMethods.get(j);
                                if (checkBuiltInMethodCall(codeBlock.get(), builtInMethod)) {
                                    counter++;
                                }
                            }
                            if (counter == builtInMethods.size()) {
                                JSONArray requiredConstructs = (JSONArray) methodConfig.get("constructs");
                                if (checkMethodConstructs(codeBlock.get(), requiredConstructs)) {
                                    System.out.println("Success");
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    private static MethodDeclaration checkMethodSignature(CompilationUnit cu, JSONObject methodConfig) {
        MethodDeclaration methodDeclaration = checkMethodName(cu, methodConfig);
        if (methodDeclaration != null && checkMethodReturnType(methodDeclaration, methodConfig)
                && checkMethodParameters(methodDeclaration, methodConfig)) {
            return methodDeclaration;
        } else {
            return null;
        }
    }

    private static MethodDeclaration checkMethodName(CompilationUnit cu, JSONObject methodConfig) {
        if (methodConfig.get("name") == null) {
            System.out.println("Problem: Objects of `requiredMethods` array must contain `name` property");
            return null;
        } else {
            JSONObject required = new JSONObject();
            required.put("name", methodConfig.get("name").toString());
            VoidVisitor<JSONObject> methodNameTester = new MethodNameTester();
            methodNameTester.visit(cu, required);
            if (required.get("success") == null) {
                required.put("success", false);
                required.put("errorCode", 210);
                required.put("md", null);
            }
            displayResult(required);
            try {
                MethodDeclaration Md = (MethodDeclaration) required.get("md");
                return Md;
            } catch (Exception exception) {
                return null;
            }
        }
    }

    private static Boolean checkMethodReturnType(MethodDeclaration md, JSONObject methodConfig) {
        String returnTypeConfig = (String) methodConfig.get("return");
        if (returnTypeConfig == null) {
            System.out.println("Problem: Objects of `requiredMethods` array must contain `return` property");
            return false;
        } else {
            JSONObject required = new JSONObject();
            required.put("returnType", returnTypeConfig);
            VoidVisitor<JSONObject> methodReturnTypeTester = new MethodReturnTypeTester();
            methodReturnTypeTester.visit(md, required);
            displayResult(required);
            return (Boolean) required.get("success");
        }
    }

    private static Boolean checkMethodParameters(MethodDeclaration md, JSONObject methodConfig) {
        JSONArray methodParametersConfig = (JSONArray) methodConfig.get("parameters");
        if (methodParametersConfig == null) {
            System.out.println("Problem: Objects of `requiredMethods` array must contain `parameters` array");
            return false;
        } else {
            JSONObject requiredParameters = new JSONObject();
            requiredParameters.put("parameters", methodParametersConfig);
            VoidVisitor<JSONObject> methodParametersTester = new MethodParametersTester();
            methodParametersTester.visit(md, requiredParameters);
            displayResult(requiredParameters);
            return (Boolean) requiredParameters.get("success");
        }
    }

    private static void checkMethodAccessModifier(MethodDeclaration methodDeclaration, JSONObject methodConfig) {
        JSONArray accessModifiersConfig = (JSONArray) methodConfig.get("accessModifiers");
        // Skip for-loop if `access modifiers` is present or not
        // int counter = 0;
        for (int j = 0; accessModifiersConfig != null && j < accessModifiersConfig.size(); j++) {
            JSONObject accessModifier = (JSONObject) accessModifiersConfig.get(j);
            JSONObject required = new JSONObject();
            if (!accessModifier.containsKey("name")) {
                System.out.println("Problem: Objects of `accessModifiers` array must contain `name` property");
                continue;
            }
            Boolean forbidden = false;
            if (accessModifier.containsKey("forbidden") && (Boolean) accessModifier.get("forbidden")) {
                forbidden = true;
            }
            required.put("accessModifierName", accessModifier.get("name").toString());
            required.put("forbidden", forbidden);
            VoidVisitor<JSONObject> methodAccessModifierTester = new MethodAccessModifierTester();
            methodAccessModifierTester.visit(methodDeclaration, required);
            displayResult(required);
            // if ((Boolean) required.get("success")) {
            // counter++;
            // }
        }
        // return counter == accessModifiersConfig.size();
    }

    private static void checkMethodOtherModifier(MethodDeclaration methodDeclaration, JSONObject methodConfig) {
        JSONArray modifiersConfig = (JSONArray) methodConfig.get("modifiers");
        for (int j = 0; modifiersConfig != null && j < modifiersConfig.size(); j++) {
            JSONObject modifierConfig = (JSONObject) modifiersConfig.get(j);
            if (!modifierConfig.containsKey("name")) {
                System.out.println("Problem: Objects of `modifiers` array must contain `name` property");
                continue;
            }
            Boolean forbidden = false;
            if (modifierConfig.containsKey("forbidden") && (Boolean) modifierConfig.get("forbidden")) {
                forbidden = true;
            }
            JSONObject required = new JSONObject();
            required.put("modifierName", modifierConfig.get("name").toString());
            required.put("forbidden", forbidden);
            VoidVisitor<JSONObject> methodOtherModifierTester = new MethodOtherModifierTester();
            methodOtherModifierTester.visit(methodDeclaration, required);
            displayResult(required);
        }
    }

    private static void checkMethodAnnotation(MethodDeclaration methodDeclaration, JSONObject methodConfig) {
        JSONArray annotationsConfig = (JSONArray) methodConfig.get("annotations");
        for (int j = 0; annotationsConfig != null && j < annotationsConfig.size(); j++) {
            JSONObject annotationConfig = (JSONObject) annotationsConfig.get(j);
            if (!annotationConfig.containsKey("name")) {
                System.out.println("Problem: Objects of `annotations` array must contain `name` property");
                continue;
            }
            Boolean forbidden = false;
            if (annotationConfig.containsKey("forbidden") && (Boolean) annotationConfig.get("forbidden")) {
                forbidden = true;
            }
            JSONObject required = new JSONObject();
            required.put("annotationName", annotationConfig.get("name").toString());
            required.put("forbidden", forbidden);
            VoidVisitor<JSONObject> methodAnnotationsTester = new MethodAnnotationsTester();
            methodAnnotationsTester.visit(methodDeclaration, required);
            displayResult(required);
        }
    }

    private static Boolean checkUserDefinedMethodCall(BlockStmt codeBlock) {

        JSONObject required = new JSONObject();
        Boolean requirement = true;
        required.put("userDefined", true);
        required.put("requirement", requirement);
        required.put("success", true);
        required.put("errorCode", 0);

        VoidVisitor<JSONObject> methodCallExprTester = new MethodCallExprTester();
        methodCallExprTester.visit(codeBlock, required);
        if (required.get("success") == null) {
            required.put("success", false);
            required.put("errorCode", 272);
            required.put("md", null);
        }
        displayResult(required);
        return (Boolean) required.get("success");
    }

    private static Boolean checkBuiltInMethodCall(BlockStmt codeBlock, JSONObject requiredMethod) {

        JSONObject required = new JSONObject();
        String requiredMethodName = requiredMethod.get("name").toString();
        Boolean requirement = true;
        if (requiredMethod.containsKey("forbidden")) {
            if ((Boolean) requiredMethod.get("forbidden")) {
                requirement = false;
            }
        }
        required.put("userDefined", false);
        required.put("requiredMethodName", requiredMethodName);
        required.put("requirement", requirement);

        VoidVisitor<JSONObject> methodCallExprTester = new MethodCallExprTester();
        methodCallExprTester.visit(codeBlock, required);
        if (required.get("success") == null) {
            required.put("success", false);
            required.put("errorCode", 275); // No Methods Called
            required.put("md", null);
        }
        displayResult(required);
        return (Boolean) required.get("success");
    }

    private static Boolean checkMethodConstructs(BlockStmt codeBlock, JSONArray requiredConstructs) {
        JSONObject required = new JSONObject();
        int successCounter = 0;
        for (int j = 0; j < requiredConstructs.size(); j++) {
            JSONObject requiredConstruct = (JSONObject) requiredConstructs.get(j);
            String requiredConstructName = requiredConstruct.get("name").toString();
            Long requiredLevel = (long) -1;// (Long) ;
            if (requiredConstruct.containsKey("level")) {
                requiredLevel = (long) requiredConstruct.get("level");
            }
            Boolean requirement = true;
            if (requiredConstruct.containsKey("forbidden")) {
                if ((Boolean) requiredConstruct.get("forbidden")) {
                    requirement = false;
                }
            }

            required.put("requiredConstructName", requiredConstructName);
            required.put("requiredLevel", requiredLevel);
            required.put("requirement", requirement);

            VoidVisitor<JSONObject> methodConstructTester = new MethodConstructTester();
            methodConstructTester.visit(codeBlock.asBlockStmt(), required);

            if (required.get("success") == null) {
                if (requirement) {
                    required.put("success", false);
                    required.put("errorCode", 290);
                } else {
                    required.put("success", true);
                    required.put("errorCode", 0);
                }
            }
            if ((Boolean) required.get("success")) {
                successCounter++;
            }
            displayResult(required);
        }
        return (requiredConstructs.size() == successCounter);
    }

    private static void displayResult(JSONObject result) {
        if (!(Boolean) result.get("success")) {
            System.out.println("Error: " + result.get("errorCode"));
            if ((Range) result.get("range") != null)
                System.out.println("Location: " + ((Range) result.get("range")).begin);
        } else {
            System.out.println("OK !");
        }

    }

    // Overriding Visit Methods

    private static class MethodNameTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodDeclaration md, JSONObject jobject) {
            super.visit(md, jobject);
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            String name = md.getNameAsString();
            if (name.equals(jobject.get("name").toString())) {
                jobject.put("success", true);
                jobject.put("errorCode", 0);
                jobject.put("range", md.getRange().get());
                jobject.put("md", md);
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 210);
                jobject.put("range", md.getRange().get());
                jobject.put("md", null);
            }
        }
    }

    private static class MethodReturnTypeTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodDeclaration md, JSONObject jobject) {
            super.visit(md, jobject);
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            String returnType = md.getTypeAsString();

            if (returnType.equals(jobject.get("returnType").toString())) {
                jobject.put("success", true);
                jobject.put("errorCode", 0);
                jobject.put("range", md.getRange().get());
                jobject.put("cu", md.findCompilationUnit());
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 220);
                jobject.put("range", md.getRange().get());
            }
        }
    }

    private static class MethodParametersTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodDeclaration md, JSONObject jobject) { // ArrayList<String> requiredParameters
            super.visit(md, jobject);
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            NodeList<com.github.javaparser.ast.body.Parameter> codeParameters = md.getParameters();
            JSONArray requiredParametersJSONArray = (JSONArray) jobject.get("parameters");
            if (requiredParametersJSONArray.size() == codeParameters.size()) {
                int matchCounter = 0;
                for (int i = 0; i < requiredParametersJSONArray.size(); i++) {
                    if (requiredParametersJSONArray.get(i).toString()
                            .equalsIgnoreCase(codeParameters.get(i).getTypeAsString())) {
                        matchCounter++;
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

            } else {
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
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            EnumSet<Modifier> modifiers = md.getModifiers();
            AccessSpecifier accessSpecifier = Modifier.getAccessSpecifier(modifiers);
            String accessSpecifierString = accessSpecifier.asString();
            Boolean forbidden = (Boolean) jobject.get("forbidden");
            String expectedModifier = jobject.get("accessModifierName").toString();
            Boolean accessModifierFound = accessSpecifierString.equals(expectedModifier);
            if ((accessModifierFound && !forbidden) || (!accessModifierFound && forbidden)) {
                jobject.put("success", true);
            } else if (accessModifierFound && forbidden) {
                jobject.put("success", false);
                jobject.put("errorCode", 240);
                System.out.println("Forbidden access modifier `" + expectedModifier + "` is present");
                // problems.add(new Problem("Forbidden access modifier `" + expectedModifier +
                // "` is present", md.getRange().get()));
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 241);
                System.out.println("Required access modifier `" + expectedModifier + "` is not present");
                // problems.add(new Problem("Required access modifier `" + expectedModifier + "`
                // is not present", md.getRange().get()));
            }
        }
    }

    private static class MethodOtherModifierTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodDeclaration md, JSONObject jobject) {
            super.visit(md, jobject);
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            EnumSet<Modifier> modifiers = md.getModifiers();
            String accessSpecifierString = Modifier.getAccessSpecifier(modifiers).toString();

            List<Modifier> otherModifiers = modifiers.stream()
                    .filter(modifier -> !modifier.toString().equals(accessSpecifierString))
                    .collect(Collectors.toList());

            Boolean forbidden = (Boolean) jobject.get("forbidden");
            String expectedModifier = jobject.get("modifierName").toString();
            Boolean modifierFound = false;
            for (Modifier modifier : otherModifiers) {
                if (modifier.asString().equalsIgnoreCase(expectedModifier)) {
                    modifierFound = true;
                    break;
                }
            }

            if ((modifierFound && !forbidden) || (!modifierFound && forbidden)) {
                jobject.put("success", true);
            } else if (modifierFound && forbidden) {
                jobject.put("success", false);
                jobject.put("errorCode", 250);
                System.out.println("Forbidden modifier `" + expectedModifier + "` is present");
                // problems.add(new Problem("Forbidden modifier `" + expectedModifier + "` is
                // present", md.getRange().get()));
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 251);
                System.out.println("Required modifier `" + expectedModifier + "` is not present");
                // problems.add(new Problem("Required modifier `" + expectedModifier + "` is not
                // present", md.getRange().get()));
            }
        }
    }

    private static class MethodAnnotationsTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodDeclaration md, JSONObject jobject) {
            super.visit(md, jobject);
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            NodeList<AnnotationExpr> annotationExprs = md.getAnnotations();
            Boolean forbidden = (Boolean) jobject.get("forbidden");
            String annotationExpected = jobject.get("annotationName").toString();
            Boolean annotationFound = false;
            for (AnnotationExpr annotationExpr : annotationExprs) {
                if (annotationExpr.getNameAsString().equalsIgnoreCase(annotationExpected)) {
                    annotationFound = true;
                    break;
                }
            }

            if ((annotationFound && !forbidden) || (!annotationFound && forbidden)) {
                jobject.put("success", true);
            } else if (annotationFound && forbidden) {
                jobject.put("success", false);
                jobject.put("errorCode", 260);
                System.out.println("Forbidden annotation `" + annotationExpected + "` is present");
                // problems.add(new Problem("Forbidden annotation `" + annotationExpected + "` is
                // present", md.getRange().get()));
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 261);
                System.out.println("Required annotation `" + annotationExpected + "` is not present");
                // problems.add(new Problem("Required annotation `" + annotationExpected + "` is not
                // present", md.getRange().get()));
            }
        }
    }

    private static class MethodCallExprTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodCallExpr methodCallExpr, JSONObject jobject) {
            super.visit(methodCallExpr, jobject);

            if ((Boolean) jobject.get("userDefined")) {
                String scope = "";
                if (methodCallExpr.getScope().isPresent()) {
                    scope = methodCallExpr.getScope().get().toString();
                }
                if (scope.isEmpty() || scope.equals("this")) { // User Defined
                    jobject.put("success", false);
                    jobject.put("errorCode", 270);
                    jobject.put("range", methodCallExpr.getRange().get());
                }
                return;
            } else { // Built in

                if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                    return;
                }

                Boolean requirement = (Boolean) jobject.get("requirement");
                String requiredMethodName = jobject.get("requiredMethodName").toString();

                if ((requiredMethodName.equals(methodCallExpr.getNameAsString())) == requirement) {
                    jobject.put("success", true);
                    jobject.put("errorCode", 0);
                    jobject.put("range", methodCallExpr.getRange().get());
                } else {
                    jobject.put("success", false);
                    jobject.put("errorCode", 280);
                    jobject.put("range", methodCallExpr.getRange().get());
                    jobject.put("cu", Optional.empty());
                }

            }

        }
    }

    private static class MethodConstructTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(BlockStmt blockStatement, JSONObject jobject) {
            super.visit(blockStatement, jobject);
            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            Boolean requirement = (Boolean) jobject.get("requirement");
            String requiredConstructName = jobject.get("requiredConstructName").toString();
            Long requiredLevel = (Long) jobject.get("requiredLevel");
            Boolean constructFound = false;

            int level = 0;
            Optional<Node> pn = blockStatement.getParentNode();
            while (pn.isPresent() && !pn.get().getClass().getSimpleName().toString().equals("MethodDeclaration")) {
                pn = pn.get().getParentNode();
                if (pn.get().getClass().getSimpleName().toString().equals("BlockStmt")) {
                    level++;
                }
            }
            if (requiredLevel == -1) {
                requiredLevel = (long) level;
            }

            NodeList nodeList = blockStatement.getStatements();
            for (Object node : nodeList) {
                String stmtSimpleName = node.getClass().getSimpleName().toString().replaceAll("Stmt", "");
                if (stmtSimpleName.equalsIgnoreCase(requiredConstructName) && requiredLevel == level) {
                    constructFound = true;
                    Statement stmt = (Statement) node;
                    jobject.put("range", stmt.getRange().get());
                }
            }

            if (constructFound) {
                if (constructFound == requirement) {
                    jobject.put("success", true);
                    jobject.put("errorCode", 0);
                } else {
                    jobject.put("success", false);
                    jobject.put("errorCode", 291);
                }
            }
        }

    }

    // private static void putResult ( JSONObject result) {}

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
