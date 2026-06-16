package org.opensilex.core.data.api.spectra;

import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import org.opensilex.fs.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;


/**
 * @author efernandez A utility class for parse CSV spectrum file 
 */

public class SpectraCSVFileParser {
    private final FileStorageService fileService;
    private static final Logger LOGGER = LoggerFactory.getLogger(SpectraCSVFileParser.class);


    public SpectraCSVFileParser(FileStorageService fileService) {
        this.fileService = fileService;
    }

    public void parseCSVFile(String inputFilePath) throws IOException, CsvException {
        BufferedReader filereader = new BufferedReader(new FileReader(inputFilePath));
        CSVParser csvParser = new CSVParserBuilder().withSeparator('\t').build();
        CSVReader csvReader = new CSVReaderBuilder(filereader).withCSVParser(csvParser).build();

        String FS_FILE_PREFIX = "datafile/spectres";

        List<String[]> allLines = csvReader.readAll();
        if (allLines.isEmpty()) {
            throw new IOException("CSV file is empty or cannot be read.");
        }

        String[] header = allLines.get(0);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0); 

        
        for (int i = 1; i < allLines.size(); i++) {
            String[] line = allLines.get(i);


            if (line.length < 1) {
                LOGGER.warn("Line is missing columns: " + String.join("\t", line));
                continue; 
            }

            String sampleName = line[0].trim();
            if (sampleName.isEmpty()) {
                LOGGER.warn("Sample name is empty on line " + (i + 1));
                continue; 
            }
 
            String formattedTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(calendar.getTime());
            String filename = String.format("%s_%s.csv", sampleName, formattedTime);
            Path tempFilePath = Files.createTempFile("csv_", ".csv");

            try (CSVWriter writer = new CSVWriter(new FileWriter(tempFilePath.toFile()))) {
                writer.writeNext(header); 
                writer.writeNext(line); 
            }

            Path targetFilePath = Paths.get(FS_FILE_PREFIX, filename);
            fileService.writeFile(FS_FILE_PREFIX, targetFilePath, tempFilePath.toFile());
            Files.deleteIfExists(tempFilePath);
            calendar.add(Calendar.SECOND, 10);
        }
    }
}