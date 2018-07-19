package hs_fulda.JavaParser;

public class App {
    public static void main(String[] args) {
        String configFilePath = "", javaCodeFilePath = "", latexFilePath = "";
        for (String commandLineArgument : args) {
            if (commandLineArgument.endsWith(".json"))
                configFilePath = commandLineArgument;
            if (commandLineArgument.endsWith(".java"))
                javaCodeFilePath = commandLineArgument;
            if (commandLineArgument.endsWith(".tex"))
                latexFilePath = commandLineArgument;
        }

        if (configFilePath.isEmpty())
            System.out.println("Please provide Configuration file path");
        if (javaCodeFilePath.isEmpty())
            System.out.println("Please provide Java code file path");
        if (!javaCodeFilePath.isEmpty() && !configFilePath.isEmpty())
            Analyzer.parseJavaFile(javaCodeFilePath, configFilePath);
        if (!latexFilePath.isEmpty())
            LatexCodeExtractor.writeCodeSnippetsToFile(latexFilePath);
    }
}
