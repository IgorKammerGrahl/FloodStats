package br.edu.floodstats.application;

import java.io.File;

public class ReportService {

    public boolean checkAndConfirmOverwrite(String outputPath, boolean forceOverwrite) {
        File file = new File(outputPath);
        if (file.exists()) {
            return forceOverwrite;
        }
        return true;
    }
}
