package hs_fulda.JavaParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class LatexCodeExtractor {

    public static void writeCodeSnippetsToFile(String texFilePath) {
        List<String> codeSnippets = readTexFile(texFilePath);
        List<String> codeSnippetsWithoutClass = new ArrayList<String>();
        List<String> codeSnippetsWithClass = new ArrayList<String>();
        List<String> codeSnippetsWithError = new ArrayList<String>();

        for (String codeSnippet : codeSnippets) {
            if (Analyzer.validCode(codeSnippet)) {
                codeSnippetsWithClass.add(codeSnippet);
            } else if (Analyzer.validCode(wrapWithClass(codeSnippet, "TestClass"))) {
                codeSnippetsWithoutClass.add(codeSnippet);
            } else {
                codeSnippetsWithError.add(commentOutCode(codeSnippet));
            }
        }

        codeSnippetsWithoutClass.addAll(codeSnippetsWithError);
        // System.out.println(mergeSnippetsInAClass(codeSnippetsWithoutClass));
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("CodeSnippetsMerged.java"), "utf-8"))) {
            writer.write(mergeSnippetsInAClass(codeSnippetsWithoutClass));
            System.out.println("Code Snippets has been merged into the file CodeSnippetsMerged.java");
        } catch (Exception ex) {
        }
    }

    public static List<String> readTexFile(String texFilePath) {
        List<String> codeSnippets = new ArrayList<>();
        String data = "";
        Boolean isReadingCode = false;
        String codeStartTag = "\\begin{verbatim}";
        String codeEndTag = "\\end{verbatim}";

        try {
            BufferedReader br = new BufferedReader(new FileReader(texFilePath));
            String fileRead = br.readLine();

            while (fileRead != null) {
                int endIndex = fileRead.indexOf(codeEndTag);

                if (endIndex > -1) {
                    isReadingCode = false;
                    codeSnippets.add(data);
                    data = "";
                }

                if (isReadingCode == true) {
                    data += "\n" + fileRead;
                }

                int beginIndex = fileRead.indexOf(codeStartTag);
                if (beginIndex > -1) {
                    isReadingCode = true;
                    data = fileRead.substring(codeStartTag.length());
                }

                fileRead = br.readLine();
            }
            br.close();
        }

        catch (FileNotFoundException fnfe) {
            System.out.println("file not found");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return codeSnippets;
    }

    public static String wrapWithClass(String javaCode, String testClassName) {
        String classStartString = "class " + testClassName + " {" + "\n" + "\tpublic static void main(String[] args) {"
                + "\n";
        String classEndString = "\n\t" + "}" + "\n" + "}" + "\n";
        return classStartString + javaCode + classEndString;
    }

    public static String mergeSnippetsInAClass(List<String> javaCodeList) {
        String javaCodeFile = "";
        for (String javaCode : javaCodeList) {
            javaCodeFile += "\n\n{\n" + javaCode + "\n}\n\n";
        }
        return wrapWithClass(javaCodeFile, "TestClass");
    }

    public static String commentOutCode(String javaCode) {
        return "/* Contains Syntax error \n" + javaCode + "\n*/";
    }

}
