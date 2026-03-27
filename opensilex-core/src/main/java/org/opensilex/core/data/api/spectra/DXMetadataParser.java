package org.opensilex.core.data.api;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.net.URI;

/**
 * @author efernandez A utility class for parse DX (JCAMP Spectroscopic Data Exchange Format) spectrum file into JSON
 */


 public class DXMetadataParser {

    public List<Map<String, Object>> parseDXFileForJSON(String inputFilePath, URI rdfType, URI provenanceUri, List<URI> experiments, String baseURIAlias) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
        List<Map<String, Object>> jsonDataList = new ArrayList<>();
        String line;
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd/MM/yy");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        String currentDate = outputDateFormat.format(new Date());
        String currentTime = timestampFormat.format(new Date()).split("T")[1];
        String currentSampleName = null;
        String currentDateField = null;
        String currentTimeField = null;
        String instrumentalParameters = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("##TITLE=")) {
                // Finalize the previous sample if any
                if (!jsonDataList.isEmpty()) {
                    Map<String, Object> lastJsonData = jsonDataList.get(jsonDataList.size() - 1);
                    String lastSampleName = (String) lastJsonData.get("sample_name");
                    String lastTime = (String) lastJsonData.get("time");
                    if (lastTime == null) {
                        lastTime = currentTime;
                    }
                    String relativePath = String.format("datafile/spectres/jdx_%s_%s.dx", lastSampleName, lastTime);
                    lastJsonData.put("relative_path", relativePath);
                }

                // Start a new sample
                Map<String, Object> jsonData = new HashMap<>();
                jsonData.put("rdf_type", rdfType);
                Map<String, Object> provenance = new HashMap<>();
                provenance.put("uri", provenanceUri);
                provenance.put("experiments", experiments);
                if (instrumentalParameters == null || instrumentalParameters.isEmpty()) {
                    provenance.put("settings", new HashMap<String, Object>());
                } else {
                    Map<String, Object> settingsMap = new HashMap<>();
                    settingsMap.put("parameter", instrumentalParameters); // Adjust this based on actual settings
                    provenance.put("settings", settingsMap);
                }                jsonData.put("provenance", provenance);
                jsonData.put("metadata", new HashMap<String, Object>());

                currentSampleName = line.split("=")[1].trim();
                jsonData.put("sample_name", currentSampleName); // For intermediate use
                jsonData.put("target", String.format("%sid/scientific-object/so-%s", baseURIAlias, currentSampleName.toLowerCase()));
                jsonDataList.add(jsonData);

                // Reset date and time for the new sample
                currentDateField = null;
                currentTimeField = null;
            } else if (line.startsWith("##DATE=")) {
                String inputDate = line.split("=")[1].trim();
                try {
                    Date parsedDate = inputDateFormat.parse(inputDate);
                    currentDateField = outputDateFormat.format(parsedDate);
                    jsonDataList.get(jsonDataList.size() - 1).put("date", currentDateField);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else if (line.startsWith("##TIME=")) {
                currentTimeField = line.split("=")[1].trim();
                Map<String, Object> currentJsonData = jsonDataList.get(jsonDataList.size() - 1);
                if (currentDateField != null) {
                    currentJsonData.put("date", String.format("%sT%s+01:00", currentDateField, currentTimeField));
                } else {
                    currentJsonData.put("date", String.format("T%s+01:00", currentTimeField));
                }
                currentJsonData.put("time", currentTimeField);
            } else if (line.startsWith("##INSTRUMENTAL PARAMETERS=")) {
                String params = line.split("=")[1].trim();
                if (params.contains("Vial")) {
                    instrumentalParameters = "vial";
                } else if (params.contains("Petri")) {
                    instrumentalParameters = "petri";
                } else {
                    instrumentalParameters = "";  
                }
            } else {
                for (String variable : new String[]{"XUNITS", "YUNITS", "XFACTOR", "YFACTOR", "FIRSTX", "LASTX", "MINY", "MAXY", "NPOINTS", "DELTAX"}) {
                    if (line.startsWith("##" + variable + "=")) {
                        String value = line.split("=")[1].trim();
                        ((Map<String, Object>) jsonDataList.get(jsonDataList.size() - 1).get("metadata")).put(variable.toLowerCase(), value);
                        break;
                    }
                }
            }
        }

        // Finalize the last sample
        if (!jsonDataList.isEmpty()) {
            Map<String, Object> lastJsonData = jsonDataList.get(jsonDataList.size() - 1);
            String lastSampleName = (String) lastJsonData.get("sample_name");
            String lastTime = (String) lastJsonData.get("time");
            if (lastTime == null) {
                lastTime = currentTime;
            }
            String relativePath = String.format("datafile/spectres/jdx_%s_%s.dx", lastSampleName, lastTime);
            lastJsonData.put("relative_path", relativePath);
        }

        reader.close();

        // Clean up the JSON data to remove 'sample_name' and 'time'
        for (Map<String, Object> data : jsonDataList) {
            data.remove("sample_name");
            data.remove("time");
        }

        return jsonDataList;
    }
}
