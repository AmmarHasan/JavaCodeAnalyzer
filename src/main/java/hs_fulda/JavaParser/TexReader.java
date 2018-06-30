package hs_fulda.JavaParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TexReader {

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
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return codeSnippets;
    }

}
