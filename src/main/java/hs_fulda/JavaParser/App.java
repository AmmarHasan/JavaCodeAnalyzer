package hs_fulda.JavaParser;

import java.util.List;

public class App {
    public static void main(String[] args) {
        // Analyzer.parseJavaFile(args[1], args[0]);
        List<String> codeSnippets = TexReader.readTexFile(args[2]);

        for (String codeSnippet : codeSnippets) {
            System.out.println();
            System.out.println(codeSnippet);
            System.out.println();
        }
    }
}
