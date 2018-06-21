package hs_fulda.JavaParser;

import java.io.File;
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

import javax.print.attribute.standard.JobName;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Range;
import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.JsonPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Analyzer {

    public static void parseJavaFile(String javaFilePath, String configFilePath) {

        JSONObject config = Analyzer.parseConfigFile(configFilePath);
        FileInputStream fileInputStream = getFileInputStream(javaFilePath);
        CombinedTypeSolver localCts = new CombinedTypeSolver();
        localCts.add(new ReflectionTypeSolver());
        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(localCts));
        JavaParser.setStaticConfiguration(parserConfiguration);
        CompilationUnit cu = JavaParser.parse(fileInputStream);

        checkClassCount(cu, config);
        JSONObject classConfig = (JSONObject) config.get("requiredClass");
        ClassOrInterfaceDeclaration classDeclaration = analyzeClass(cu, classConfig);
        if (classDeclaration != null) {
            checkMethods(classDeclaration, classConfig);
        }
    }

    public static void parseJavaCode(String javaCode, String configFilePath) {
        JSONObject config = Analyzer.parseConfigFile(configFilePath);
        CompilationUnit cu = JavaParser.parse(javaCode);
        ClassOrInterfaceDeclaration classDeclaration = analyzeClass(cu, config);
        if (classDeclaration != null) {
            // Check Method
        }
    }

    private static void checkClassCount(CompilationUnit cu, JSONObject config) {
        if (config.get("maxNumberOfClasses") != null) {
            long maxClassCount = (long) config.get("maxNumberOfClasses");
            int currentClassCount = countClasses(cu);
            if (currentClassCount > maxClassCount) {
                System.out.println("Error : Program contains more classes than permitted.");
            }
        }
    }

    // Class Parsing Methods

    private static String currentClassName = new String();

    @SuppressWarnings("unchecked")
    private static ClassOrInterfaceDeclaration analyzeClass(CompilationUnit cu, JSONObject config) {
        if (config.get("name") == null) {
            System.out.println("Problem - Config: Object of `requiredClass` must contain `name` property");
            return null;
        }
        String req_ClassName = config.get("name").toString();
        int status = 0;
        List<Integer> currentStatus = new ArrayList<Integer>();
        Stream<ClassOrInterfaceDeclaration> stream_Class = getFilteredClassStream(cu, req_ClassName);

        if (checkForClass(stream_Class)) {
            currentClassName = req_ClassName;
            stream_Class = getFilteredClassStream(cu, req_ClassName);
            ClassOrInterfaceDeclaration classNode = stream_Class.findFirst().get();
            // System.out.println("Found Class: " + classNode.getNameAsString());

            // Check Class Access Modifier
            JSONArray requiredConstructs = (JSONArray) config.get("accessModifier");
            if (requiredConstructs != null) {
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
            }

            // Checking Class Modifier
            JSONArray neededConstructs = (JSONArray) config.get("modifier");
            if (neededConstructs != null) {
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
            }

            // Checking for Super Class
            JSONArray req_SuperClass = (JSONArray) config.get("parentClass");
            if (req_SuperClass != null) {
                for (Object requiredConstruct : req_SuperClass) {
                    JSONObject req_Construct = (JSONObject) requiredConstruct;
                    String req_ConstructName = req_Construct.get("name").toString();

                    String req_ConstructRule = req_Construct.getOrDefault("forbidden", false).toString();
                    // String req_ConstructRule = req_Construct.get("forbidden").toString();
                    String compSuper = classNode.getExtendedTypes().toString();

                    if (isContain(compSuper, req_ConstructName) == true
                            && req_ConstructRule.equalsIgnoreCase("false")) {
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
            }

            // Checking for Interface
            JSONArray req_Interfaces = (JSONArray) config.get("interfaceToImplement");
            if (req_Interfaces != null) {

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
            }

            if (currentStatus.contains(2)) {
                System.out.println("Test Status: Failed");
                return null;
            } else {
                // System.out.println("Test Status: Successful");
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

    private static int countClasses(CompilationUnit cu) {
        return (int) cu.findAll(ClassOrInterfaceDeclaration.class).stream().count();
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

    private static JSONObject parseConfigFile(String configFilePath) {
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(new FileReader(configFilePath));
            // System.out.println(config.get("requiredClass").toString());
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            System.out.println("No config file was provided.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getErrorString(int errorCode) {
        JSONParser parser = new JSONParser();
        try {
            String path = new File("").getAbsolutePath().concat("/ErrorStrings.json");
            JSONObject errorStrings = (JSONObject) parser.parse(new FileReader(path));
            return (errorStrings.get(Integer.toString(errorCode)) != null)
                    ? (String) errorStrings.get(Integer.toString(errorCode))
                    : null;
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            return "No error description file was provided.";
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

    // Method Parsing Methods

    private static void checkMethods(ClassOrInterfaceDeclaration cd, JSONObject config) {

        JSONArray requiredMethodsConfig = (JSONArray) config.get("requiredMethods");
        for (int i = 0; requiredMethodsConfig != null && i < requiredMethodsConfig.size(); i++) {
            // Each object inside `requiredMethods` array
            JSONObject methodConfig = (JSONObject) requiredMethodsConfig.get(i);
            MethodDeclaration methodDeclaration = checkMethodSignature(cd, methodConfig);
            if (methodDeclaration != null) {
                checkMethodAccessModifier(methodDeclaration, methodConfig);
                checkMethodOtherModifier(methodDeclaration, methodConfig);
                checkMethodAnnotation(methodDeclaration, methodConfig);
                checkMethodCalls(methodDeclaration.getBody().get(), methodConfig, "Method");
                checkMethodConstructs(methodDeclaration.getBody().get(), methodConfig, "Method");
                checkOperators(methodDeclaration, methodConfig);
            }
        }
    }

    private static MethodDeclaration checkMethodSignature(ClassOrInterfaceDeclaration cd, JSONObject methodConfig) {
        MethodDeclaration methodDeclaration = checkMethodName(cd, methodConfig);
        if (methodDeclaration != null) {
            int successCounter = 0;
            if (checkMethodReturnType(methodDeclaration, methodConfig)) {
                successCounter++;
            }
            if (checkMethodParameters(methodDeclaration, methodConfig)) {
                successCounter++;
            }
            if (successCounter == 2) {
                return methodDeclaration;
            }
        }
        return null;
    }

    private static MethodDeclaration checkMethodName(ClassOrInterfaceDeclaration cd, JSONObject methodConfig) {
        if (methodConfig.get("name") == null) {
            System.out.println("Problem: Objects of `requiredMethods` array must contain `name` property");
            return null;
        } else {
            JSONObject required = new JSONObject();
            required.put("name", methodConfig.get("name").toString());
            VoidVisitor<JSONObject> methodNameTester = new MethodNameTester();
            methodNameTester.visit(cd, required);
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

    private static void checkMethodCalls(BlockStmt codeBlock, JSONObject methodConfig, String context) {

        JSONArray methodCallsConfig = (JSONArray) methodConfig.get("methodCalls");
        for (int j = 0; methodCallsConfig != null && j < methodCallsConfig.size(); j++) {
            JSONObject methodCallConfig = (JSONObject) methodCallsConfig.get(j);
            JSONObject required = new JSONObject();
            if (!methodCallConfig.containsKey("className")) {
                System.out.println("Problem: Objects of `methodCalls` array must contain `className` property");
                return;
            }
            String className = methodCallConfig.get("className").toString();
            required.put("className", className);

            Boolean isCurrentClass = ((currentClassName.equalsIgnoreCase(className)) || className.isEmpty()
                    || className.equalsIgnoreCase("this"));
            required.put("isCurrentClass", isCurrentClass);

            if (methodCallConfig.containsKey("methodName")) {
                required.put("methodName", methodCallConfig.get("methodName").toString());
            }
            Boolean requirement = true;
            if (methodCallConfig.containsKey("forbidden")) {
                if ((Boolean) methodCallConfig.get("forbidden")) {
                    requirement = false;
                }
            }
            required.put("requirement", requirement);
            required.put("context", context);

            VoidVisitor<JSONObject> methodCallsExprTester = new MethodCallsExprTester();
            methodCallsExprTester.visit(codeBlock, required);

            if (required.get("success") == null) {
                if (requirement) {
                    required.put("success", false);
                    required.put("errorCode", 272); // Required Method but not found
                    System.out.println("Required Method Call is not present" + " - Context: " + context);
                } else {
                    required.put("success", true);
                    required.put("errorCode", 0);
                }
            } else if ((int) required.get("errorCode") == 272) {
                // if (jobject.containsKey("methodName")) {
                // requiredMethodName = jobject.get("methodName").toString();
                // }
                System.out.println("Required Method Call "
                        + ((required.containsKey("methodName")) ? "'" + required.get("methodName").toString() + "' "
                                : "")
                        + "is not present" + " - Context: " + context);
            }
            // displayResult(required);
        }
    }

    private static void checkOperators(MethodDeclaration methodDeclaration, JSONObject methodConfig) {
        JSONArray operatorsConfig = (JSONArray) methodConfig.get("operators");
        for (int j = 0; operatorsConfig != null && j < operatorsConfig.size(); j++) {
            JSONObject operatorConfig = (JSONObject) operatorsConfig.get(j);
            if (!operatorConfig.containsKey("name")) {
                System.out.println("Problem: Objects of `operators` array must contain `name` property");
                continue;
            }
            Boolean forbidden = false;
            if (operatorConfig.containsKey("forbidden") && (Boolean) operatorConfig.get("forbidden")) {
                forbidden = true;
            }
            JSONObject required = new JSONObject();
            Long level = (long) -1;// (Long) ;
            if (operatorConfig.containsKey("level")) {
                level = (long) operatorConfig.get("level");
            }

            required.put("level", level);
            required.put("operatorName", operatorConfig.get("name").toString());
            required.put("forbidden", forbidden);

            VoidVisitor<JSONObject> methodOperatorsTester = new MethodOperatorsTester();
            methodOperatorsTester.visit(methodDeclaration, required);
        }
    }

    @Deprecated
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

    @Deprecated
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
            if (requirement) {
                required.put("success", false);
                required.put("errorCode", 275); // No Methods Called
                required.put("md", null);
            } else {
                required.put("success", true);
                required.put("errorCode", 0); // No Methods Called
                required.put("md", null);
            }
        }
        displayResult(required);
        return (Boolean) required.get("success");
    }

    private static void checkMethodConstructs(BlockStmt codeBlock, JSONObject methodConfig, String context) {

        JSONArray requiredConstructs = (JSONArray) methodConfig.get("constructs");
        if (requiredConstructs != null) {
            JSONObject required = new JSONObject();
            for (int j = 0; j < requiredConstructs.size(); j++) {
                JSONObject requiredConstruct = (JSONObject) requiredConstructs.get(j);
                if (!requiredConstruct.containsKey("name")) {
                    System.out.println("Problem: Objects of `constructs` array must contain `name` property");
                    continue;
                }
                String requiredConstructName = requiredConstruct.get("name").toString();
                Long requiredLevel = (long) -1;
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
                required.put("requiredConstruct", requiredConstruct);
                required.put("context", context);

                VoidVisitor<JSONObject> methodConstructTester = new MethodConstructTester();
                methodConstructTester.visit(codeBlock.asBlockStmt(), required);

                if (required.get("success") == null) {
                    if (requirement) {
                        required.put("success", false);
                        required.put("errorCode", 290);
                        System.out.println("Required Construct not found" + " - Context: " + context);
                    } else {
                        required.put("success", true);
                        required.put("errorCode", 0);
                    }
                }
                // displayResult(required);
            }
        }

    }

    private static void displayResult(JSONObject result) {
        if (!(Boolean) result.get("success")) {

            System.out.println("Error Code: " + result.get("errorCode"));
            String errorString = getErrorString((int) result.get("errorCode"));
            if (errorString != null) {
                System.out.println("Error Message: " + errorString);
            }

            if ((Range) result.get("range") != null)
                System.out.println("Location: " + ((Range) result.get("range")).begin);
        } else {
            // System.out.println("OK !");
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
                jobject.put("range", md.getRange().get());
                System.out.println("Forbidden access modifier `" + expectedModifier + "` is present");
                // problems.add(new Problem("Forbidden access modifier `" + expectedModifier +
                // "` is present", md.getRange().get()));
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 241);
                jobject.put("range", md.getRange().get());
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
                jobject.put("range", md.getRange().get());
                System.out.println("Forbidden modifier `" + expectedModifier + "` is present");
                // problems.add(new Problem("Forbidden modifier `" + expectedModifier + "` is
                // present", md.getRange().get()));
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 251);
                jobject.put("range", md.getRange().get());
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
                jobject.put("range", md.getRange().get());
                System.out.println("Forbidden annotation `" + annotationExpected + "` is present");
                // problems.add(new Problem("Forbidden annotation `" + annotationExpected + "`
                // is present", md.getRange().get()));
            } else {
                jobject.put("success", false);
                jobject.put("errorCode", 261);
                jobject.put("range", md.getRange().get());
                System.out.println("Required annotation `" + annotationExpected + "` is not present");
                // problems.add(new Problem("Required annotation `" + annotationExpected + "` is
                // not present", md.getRange().get()));
            }
        }
    }

    private static class MethodOperatorsTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodDeclaration methodDeclaration, JSONObject jobject) {
            super.visit(methodDeclaration, jobject);

            if (jobject.containsKey("success") && (Boolean) jobject.get("success")) {
                return;
            }

            long requiredLevel = (long) jobject.get("level");
            int level = 0;
            Boolean forbidden = (Boolean) jobject.get("forbidden");
            String operatorExpected = jobject.get("operatorName").toString();
            Boolean operatorFound = false;
            List<Boolean> requiredOperatorsFound = new ArrayList<Boolean>();

            List<BinaryExpr> binaryOperatorExpressions = methodDeclaration.getChildNodesByType(BinaryExpr.class);
            for (BinaryExpr binaryOperatorExpression : binaryOperatorExpressions) {
                if (binaryOperatorExpression.getOperator().asString().equals(operatorExpected)) {
                    Optional<Node> pn = binaryOperatorExpression.getAncestorOfType(BlockStmt.class).get()
                            .getParentNode();
                    level = 0;
                    while (pn.isPresent()
                            && !pn.get().getClass().getSimpleName().toString().equals("MethodDeclaration")) {
                        pn = pn.get().getParentNode();
                        if (pn.get().getClass().getSimpleName().toString().equals("BlockStmt")) {
                            level++;
                        }
                    }
                    operatorFound = binaryOperatorExpression.getOperator().asString().equals(operatorExpected)
                            && (requiredLevel == -1 || requiredLevel == level);
                    if ((operatorFound && !forbidden) || (!operatorFound && forbidden)) {
                        jobject.put("success", true);
                        requiredOperatorsFound.add(true);
                    } else if (operatorFound && forbidden) {
                        jobject.put("success", false);
                        jobject.put("errorCode", 310);
                        requiredOperatorsFound.add(true);
                        System.out.println("Forbidden operator `" + operatorExpected + "` is present "
                                + (requiredLevel == -1 ? "" : "at level: " + requiredLevel));
                        jobject.put("range", binaryOperatorExpression.getRange().get());
                        displayResult(jobject);
                    } else {
                        jobject.put("success", false);
                        jobject.put("errorCode", 311);
                    }
                }
            }

            List<UnaryExpr> unaryOperatorExpressions = methodDeclaration.getChildNodesByType(UnaryExpr.class);
            for (UnaryExpr unaryOperatorExpression : unaryOperatorExpressions) {
                if (unaryOperatorExpression.getOperator().asString().equals(operatorExpected)) {
                    Optional<Node> pn = unaryOperatorExpression.getAncestorOfType(BlockStmt.class).get()
                            .getParentNode();
                    level = 0;
                    while (pn.isPresent()
                            && !pn.get().getClass().getSimpleName().toString().equals("MethodDeclaration")) {
                        pn = pn.get().getParentNode();
                        if (pn.get().getClass().getSimpleName().toString().equals("BlockStmt")) {
                            level++;
                        }
                    }
                    operatorFound = unaryOperatorExpression.getOperator().asString().equals(operatorExpected)
                            && (requiredLevel == -1 || requiredLevel == level);

                    if ((operatorFound && !forbidden) || (!operatorFound && forbidden)) {
                        jobject.put("success", true);
                        requiredOperatorsFound.add(true);
                    } else if (operatorFound && forbidden) {
                        jobject.put("success", false);
                        jobject.put("errorCode", 310);
                        requiredOperatorsFound.add(true);
                        System.out.println("Forbidden operator `" + operatorExpected + "` is present "
                                + (requiredLevel == -1 ? "" : "at level: " + requiredLevel));
                        jobject.put("range", unaryOperatorExpression.getRange().get());
                        displayResult(jobject);
                    } else {
                        jobject.put("success", false);
                        jobject.put("errorCode", 311);
                    }
                }
            }

            List<ConditionalExpr> conditionalOperatorExpressions = methodDeclaration
                    .getChildNodesByType(ConditionalExpr.class);

            for (ConditionalExpr conditionalOperatorExpression : conditionalOperatorExpressions) {
                if (operatorExpected.equals("?")) {
                    Optional<Node> pn = conditionalOperatorExpression.getAncestorOfType(BlockStmt.class).get()
                            .getParentNode();
                    level = 0;
                    while (pn.isPresent()
                            && !pn.get().getClass().getSimpleName().toString().equals("MethodDeclaration")) {
                        pn = pn.get().getParentNode();
                        if (pn.get().getClass().getSimpleName().toString().equals("BlockStmt")) {
                            level++;
                        }
                    }
                    operatorFound = operatorExpected.equals("?") && (requiredLevel == -1 || requiredLevel == level);

                    if ((operatorFound && !forbidden) || (!operatorFound && forbidden)) {
                        jobject.put("success", true);
                        requiredOperatorsFound.add(true);
                    } else if (operatorFound && forbidden) {
                        jobject.put("success", false);
                        jobject.put("errorCode", 310);
                        requiredOperatorsFound.add(true);
                        System.out.println("Forbidden operator `" + operatorExpected + "` is present "
                                + (requiredLevel == -1 ? "" : "at level: " + requiredLevel));
                        jobject.put("range", conditionalOperatorExpression.getRange().get());
                        displayResult(jobject);
                    } else {
                        jobject.put("success", false);
                        jobject.put("errorCode", 311);
                    }
                }
            }

            if (!(requiredOperatorsFound.stream().anyMatch(x -> x == true))) {
                System.out.println("Required operator `" + operatorExpected + "` is present but not "
                        + (requiredLevel == -1 ? "" : "at level: " + requiredLevel));
            }
        }
    }

    @Deprecated
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

    private static class MethodCallsExprTester extends VoidVisitorAdapter<JSONObject> {

        @Override
        public void visit(MethodCallExpr methodCallExpr, JSONObject jobject) {
            super.visit(methodCallExpr, jobject);
            Boolean requirement = (Boolean) jobject.get("requirement");
            if (jobject.containsKey("success") && (Boolean) jobject.get("success") && requirement) {
                return;
            }

            Boolean isCurrentClass = (Boolean) jobject.get("isCurrentClass");
            String requiredClassName = jobject.get("className").toString();
            String context = jobject.get("context").toString();
            String requiredMethodName = null;

            String callerName = "";
            if (methodCallExpr.getScope().isPresent()) {
                if (methodCallExpr.getScope().get().isNameExpr()) {
                    try {
                        callerName = methodCallExpr.getScope().get().asNameExpr().resolve().getType().asReferenceType()
                                .getQualifiedName();
                        callerName = callerName.lastIndexOf('.') != -1
                                ? (callerName.substring(callerName.lastIndexOf('.') + 1, callerName.length()))
                                : callerName;
                    } catch (Exception e) {
                        callerName = methodCallExpr.getScope().get().toString();
                    }
                } else {
                    callerName = methodCallExpr.getScope().get().toString();
                }
            }

            // methodCallExpr.
            // System.out.println(callerName);

            if (((callerName.isEmpty() || callerName.equals("this")) && isCurrentClass) // Current Class
                    || callerName.equals(requiredClassName)) { // Class name
                if (jobject.containsKey("methodName")) {
                    requiredMethodName = jobject.get("methodName").toString();

                    // System.out.println("r: "+requiredMethodName+" p:
                    // "+methodCallExpr.getNameAsString());
                    if ((requiredMethodName.equals(methodCallExpr.getNameAsString()))) {
                        if (requirement) {
                            jobject.put("success", true);
                            jobject.put("errorCode", 0);
                            jobject.put("range", methodCallExpr.getRange().get());
                        } else {
                            jobject.put("success", false);
                            jobject.put("errorCode", 273);
                            jobject.put("range", methodCallExpr.getRange().get());
                            jobject.put("cu", Optional.empty());
                            System.out.println("Forbidden Method Call"
                                    + (requiredMethodName != null ? " '" + requiredMethodName + "'" : "")
                                    + " is found at: " + methodCallExpr.getRange().get().begin + " - Context: "
                                    + context);

                        }
                    } else {
                        if (requirement) {
                            jobject.put("success", false);
                            jobject.put("errorCode", 272);
                            // System.out.println("Required Method Call `" + requiredMethodName + "` is not
                            // present"+ " - Context: "+ context);
                            // jobject.put("range", methodCallExpr.getRange().get());
                            // jobject.put("cu", Optional.empty());
                        } else {
                            jobject.put("success", true);
                            jobject.put("errorCode", 0);
                            jobject.put("range", methodCallExpr.getRange().get());
                        }
                    }
                } else {

                    if (requirement) {
                        jobject.put("success", true);
                        jobject.put("errorCode", 0);
                        jobject.put("range", methodCallExpr.getRange().get());
                    } else {
                        jobject.put("success", false);
                        jobject.put("errorCode", 273);
                        jobject.put("range", methodCallExpr.getRange().get());
                        jobject.put("cu", Optional.empty());
                        System.out.println("Forbidden Method Call"
                                + (requiredMethodName != null ? " '" + requiredMethodName + "'" : "") + " is found at: "
                                + methodCallExpr.getRange().get().begin + " - Context: " + context);

                    }
                }
            } else {

                if (!requirement) {
                    jobject.put("success", true);
                    jobject.put("errorCode", 0);
                    jobject.put("range", methodCallExpr.getRange().get());
                } else {
                    if (jobject.containsKey("methodName")) {
                        requiredMethodName = jobject.get("methodName").toString();
                    }
                    jobject.put("success", false);
                    jobject.put("errorCode", 272);
                    // System.out.println("Required Method Callm `" + requiredMethodName + "` is not
                    // present"+ " - Context: "+ context);
                    // jobject.put("range", methodCallExpr.getRange().get());
                    // jobject.put("cu", Optional.empty());
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
            String context = jobject.get("context").toString();
            Boolean constructFound = false;

            int level = 0;
            Optional<Node> pn = blockStatement.getParentNode();
            while (pn.isPresent() && !pn.get().getClass().getSimpleName().toString().contains(context)) {
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
                // System.out.println("sn: " + stmtSimpleName +requiredConstructName);
                if (stmtSimpleName.equalsIgnoreCase(requiredConstructName) && requiredLevel == level) {
                    constructFound = true;
                    Statement stmt = (Statement) node;

                    // System.out.println("found");
                    checkMethodCalls(getBlockStmt(stmt), (JSONObject) jobject.get("requiredConstruct"), stmtSimpleName);
                    checkMethodConstructs(getBlockStmt(stmt), (JSONObject) jobject.get("requiredConstruct"),
                            stmtSimpleName);
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
                    System.out.println("Required Construct rules dont match" + "- Context: " + context);
                }
            }
        }

    }

    private static BlockStmt getBlockStmt(Statement stmt) {
        if (stmt.isBlockStmt()) {
            return stmt.asBlockStmt();
        }
        if (stmt.isForStmt()) {
            return stmt.asForStmt().getBody().asBlockStmt();
        }
        if (stmt.isIfStmt()) {
            return stmt.asIfStmt().getThenStmt().asBlockStmt();
        }
        if (stmt.isWhileStmt()) {
            return stmt.asWhileStmt().getBody().asBlockStmt();
        }
        if (stmt.isDoStmt()) {
            return stmt.asDoStmt().getBody().asBlockStmt();
        }
        if (stmt.isWhileStmt()) {
            return stmt.asWhileStmt().getBody().asBlockStmt();
        }
        return null;
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
