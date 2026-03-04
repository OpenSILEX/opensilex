package org.opensilex.core.data.api;

import java.io.*;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
/**
 * @author efernandez A utility class for parse DX spectrum file 
*/

public class DXToTSVConverter {

    /**
     * Converts a DX (JCAMP Spectroscopic Data Exchange Format) file into a TSV (Tab-Separated Values) file.

     * @param inputFile           the path to the input DX file
     * @param outputFile          the path to the output TSV file to create
     * @param includeAverage      if true, an additional row containing the average of all samples
     *                            will be added for each sample group
     * @param includeSampleDatetime if true, the sample datetime will be included as the first column
     * @throws IOException        if an I/O error occurs while reading the input file or writing the output file
     * @throws IllegalArgumentException if the x-units or y-units are inconsistent across the file
     *
     */
    public void convertDXFilesToTSV(java.nio.file.Path inputFile, java.nio.file.Path outputFile, boolean includeAverage, boolean includeSampleDatetime) throws IOException {
        Map<String, List<List<Double>>> sampleDataMap = new LinkedHashMap<>();
        Map<String, List<String>> sampleDateMap = new LinkedHashMap<>(); 
        double firstX = 0;
        double lastX = 0;
        double deltaX = 0;
        double yFactor = 1;

        BufferedReader reader = new BufferedReader(new FileReader(inputFile.toFile()));
        String line;
        List<String> xunits = new ArrayList<>();
        List<String> yunits = new ArrayList<>();
        String xunit = null;
        String yunit = null;
        String sampleName = null;
        List<Double> currentSampleData = new ArrayList<>();
        boolean dataStarted = false;
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateField = null;
        String currentTimeField = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("##FIRSTX=")) {
                firstX = Double.parseDouble(line.split("=")[1].trim());
            } else if (line.startsWith("##LASTX=")) {
                lastX = Double.parseDouble(line.split("=")[1].trim());
            } else if (line.startsWith("##DELTAX=")) {
                deltaX = Double.parseDouble(line.split("=")[1].trim());
            } else if (line.startsWith("##YFACTOR=")) {
                yFactor = Double.parseDouble(line.split("=")[1].trim());
            } else if (line.startsWith("##XUNITS=")) {
                xunit = line.split("=")[1].trim().toLowerCase();
                xunits.add(xunit);
            } else if (line.startsWith("##YUNITS=")) {
                yunit = line.split("=")[1].trim().toLowerCase();
                yunits.add(yunit);
            } else if (line.startsWith("##TITLE=")) {

                if (!currentSampleData.isEmpty()) {
                    sampleDataMap.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(currentSampleData);
                    if (includeSampleDatetime && currentDateField != null) {
                        sampleDateMap.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(currentDateField); 
                    }
                    currentSampleData = new ArrayList<>();
                }
                sampleName = line.split("=")[1].trim();
                currentDateField = null;
            } else if (line.startsWith("##XYDATA=")) {
                dataStarted = true;
            } else if (line.startsWith("##END=")) {
                if (sampleName != null && !currentSampleData.isEmpty()) {
                    sampleDataMap.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(currentSampleData);
                    if (includeSampleDatetime && currentDateField != null) {
                        sampleDateMap.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(currentDateField);
                    }
                }
                dataStarted = false;
                currentSampleData = new ArrayList<>();
            } else if (dataStarted && sampleName != null) {
                String[] values = line.trim().split("\\s+");
                for (int i = 1; i < values.length; i++) {
                    Double dataValue = Double.parseDouble(values[i]) * yFactor;
                    currentSampleData.add(dataValue);
                }
            } else if (line.startsWith("##DATE=")) {
                String inputDate = line.split("=")[1].trim();
                try {
                    Date parsedDate = inputDateFormat.parse(inputDate);
                    currentDateField = outputDateFormat.format(parsedDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (line.startsWith("##TIME=")) {
                currentTimeField = line.split("=")[1].trim();
                if (currentDateField != null) {
                    currentDateField = String.format("%sT%s+01:00", currentDateField, currentTimeField);
                }
            }
        }

        reader.close();

        if (!currentSampleData.isEmpty()) {
            sampleDataMap.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(currentSampleData);
            if (includeSampleDatetime && currentDateField != null) {
                sampleDateMap.computeIfAbsent(sampleName, k -> new ArrayList<>()).add(currentDateField); 
            }
        }

        if (!areAllElementsEqual(xunits)) {
            throw new IllegalArgumentException("Inconsistent x-units found in the input file.");
        }

        if (!areAllElementsEqual(yunits)) {
            throw new IllegalArgumentException("Inconsistent y-units found in the input file.");
        }

        if (xunits.isEmpty() || yunits.isEmpty()) {
            throw new IllegalArgumentException("Missing x-units or y-units in the input file. File is not in expected format.");
        }

        xunit = xunits.get(0);
        yunit = yunits.get(0);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            if (includeSampleDatetime) {
                writer.write("Datetime\t");
            }
            writer.write("Sample_" + xunit + "_" + yunit + "\t");
            for (double x = firstX; x <= lastX; x += deltaX) {
                writer.write(String.format("%.2f", x) + "\t");
            }
            writer.write("\n");

            for (Map.Entry<String, List<List<Double>>> entry : sampleDataMap.entrySet()) {
                String sampleNameFull = entry.getKey();
                List<List<Double>> samples = entry.getValue();
                List<String> dateTimes = sampleDateMap.get(sampleNameFull);

                for (int i = 0; i < samples.size(); i++) {
                    if (includeSampleDatetime && dateTimes != null && i < dateTimes.size()) {
                        writer.write(dateTimes.get(i) + "\t"); 
                    }
                    String sampleNameOutput = sampleNameFull + "_" + (i + 1);
                    writer.write(sampleNameOutput + "\t");
                    List<Double> data = samples.get(i);
                    for (Double value : data) {
                        writer.write(String.format("%.6f", value) + "\t");
                    }
                    writer.write("\n");
                }

                if (includeAverage) {
                    List<Double> avgSample = new ArrayList<>();
                    for (int col = 0; col < samples.get(0).size(); col++) {
                        double colSum = 0;
                        for (List<Double> sample : samples) {
                            colSum += sample.get(col);
                        }
                        double avgValue = colSum / samples.size();
                        avgSample.add(avgValue);
                    }
                    if (includeSampleDatetime) {
                        writer.write("\t");
                    }

                    writer.write(sampleNameFull + "_average\t");
                    for (Double value : avgSample) {
                        writer.write(String.format("%.6f", value) + "\t");
                    }
                    writer.write("\n");
                }
            }
        }
    }

    // Method to check if all units are equal
    private boolean areAllElementsEqual(List<String> list) {
        if (list.isEmpty()) {
            return true;
        }
        String firstElement = list.get(0);
        for (String element : list) {
            if (!firstElement.equals(element)) {
                return false;
            }
        }
        return true;
    }
}