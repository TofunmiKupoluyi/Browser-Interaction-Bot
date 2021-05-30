package com.tkupoluyi.browser_interaction_bot;

import java.io.FileWriter;
import java.io.IOException;

public class DOTFileBuilder {
    StringBuilder fileStringBuilder;
    FileWriter outputFileWriter;
    String outputDirectory;

    DOTFileBuilder(String outputDirectory) {
        fileStringBuilder = new StringBuilder();
        fileStringBuilder.append("digraph eventGraph {\n");
        this.outputDirectory = outputDirectory;
    }

    public void addNode(String dotRepresentation) {
        fileStringBuilder.append(dotRepresentation+"\n");
    }

    public void close() {
        fileStringBuilder.append("}");
        try {
            outputFileWriter = new FileWriter(outputDirectory + "/output.dot");
            outputFileWriter.write(fileStringBuilder.toString());
            outputFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
