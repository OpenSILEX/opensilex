package org.opensilex.core.data.api.spectra;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author efernandez A utility class for parse CSV spectrum file into JSON
 */

public class SpectraCSVMetadataParser {

    public List<Map<String, Object>> parseSpectraCSVFileForJSON(String inputFilePath, URI rdfType, URI provenanceUri, List<URI> experiments, String baseURIAlias) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
        List<Map<String, Object>> jsonDataList = new ArrayList<>();
        String line;
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.SECOND, 0);

        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IOException("CSV file is empty or cannot be read.");
        }

        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length < 1) {
                continue; 
            }

            String formattedTime = timestampFormat.format(calendar.getTime());
            String sampleName = columns[0].trim();
            String target = String.format("%sid/scientific-object/so-%s", baseURIAlias, sampleName.toLowerCase());

            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("rdf_type", rdfType);
            Map<String, Object> provenance = new HashMap<>();
            provenance.put("uri", provenanceUri);
            provenance.put("experiments", experiments);
            jsonData.put("provenance", provenance);
            jsonData.put("sample_name", sampleName);
            jsonData.put("target", target);
            jsonData.put("date", formattedTime);

            String filename = String.format("%s_%s.csv", sampleName, formattedTime);

            String relativePath = String.format("datafile/spectres/%s", filename);
            jsonData.put("relative_path", relativePath);

            jsonDataList.add(jsonData);

            calendar.add(Calendar.SECOND, 10);
        }

        reader.close();

        for (Map<String, Object> data : jsonDataList) {
            data.remove("sample_name");
        }

        return jsonDataList;
    }
}