package org.opensilex.core.data.api.spectra;

import org.opensilex.fs.service.FileStorageService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;


/**
 * @author efernandez A utility class for parse DX (JCAMP Spectroscopic Data Exchange Format) spectrum file 
 * This class splits the insert file into as many files as there are samples present in order to download datafiles separately
 */

 public class DXFileParser {
    private final FileStorageService fileService;
    private static final String FS_FILE_PREFIX = "datafile/spectres";

    public DXFileParser(FileStorageService fileService) {
        this.fileService = fileService;
    }

    public void parseDXfile(String inputFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            String currentSampleName = null;
            String currentTime = null;
            StringBuilder currentContent = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("##TITLE=")) {
                    handleFileWrite(currentSampleName, currentTime, currentContent);
                    currentSampleName = extractValue(line);
                } else if (line.startsWith("##TIME=")) {
                    currentTime = extractValue(line);
                }
                currentContent.append(line).append("\n");
            }
            
            handleFileWrite(currentSampleName, currentTime, currentContent);
        }
    }

    private void handleFileWrite(String sampleName, String time, StringBuilder content) throws IOException {
        if (content.length() > 0 && sampleName != null) {
            String filename = String.format("jdx_%s_%s.dx", sampleName, time);
            java.nio.file.Path filePath = Paths.get(FS_FILE_PREFIX, filename);

            fileService.writeFile(FS_FILE_PREFIX, filePath, content.toString().getBytes());

            content.setLength(0); 
        }
    }

    private String extractValue(String line) {
        return line.split("=")[1].trim();
    }
}