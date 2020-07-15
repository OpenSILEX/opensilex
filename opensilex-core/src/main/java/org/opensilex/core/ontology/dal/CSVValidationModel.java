/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author vmigot
 */
public class CSVValidationModel {

    private List<String> missingHeaders = new ArrayList<>();

    private Map<Integer, List<CSVDatatypeError>> datatypeErrors = new HashMap<>();

    private Map<Integer, List<CSVURINotFoundError>> uriNotFoundErrors = new HashMap<>();

    private Map<Integer, List<CSVInvalidURIError>> invalidURIErrors = new HashMap<>();
    
    private Map<Integer, List<CSVMissingRequiredValueError>> missingRequiredValueErrors = new HashMap<>();

    public List<String> getMissingHeaders() {
        return missingHeaders;
    }

    public Map<Integer, List<CSVDatatypeError>> getDatatypeErrors() {
        return datatypeErrors;
    }

    public Map<Integer, List<CSVURINotFoundError>> getUriNotFoundErrors() {
        return uriNotFoundErrors;
    }

    public Map<Integer, List<CSVInvalidURIError>> getInvalidURIErrors() {
        return invalidURIErrors;
    }

    public Map<Integer, List<CSVMissingRequiredValueError>> getMissingRequiredValueErrors() {
        return missingRequiredValueErrors;
    }
    
    public boolean hasErrors() {
        return getMissingHeaders().size() > 0
                || getDatatypeErrors().size() > 0
                || getUriNotFoundErrors().size() > 0
                || getInvalidURIErrors().size() > 0
                || getMissingRequiredValueErrors().size() > 0;
    }

    public void addMissingHeaders(Collection<String> headers) {
        missingHeaders.addAll(headers);
    }

    public void addInvalidDatatypeError(int rowIndex, int colIndex, String header, BuiltInDatatypes dataType, String value) {
        if (!datatypeErrors.containsKey(rowIndex)) {
            datatypeErrors.put(rowIndex, new ArrayList<>());
        }
        datatypeErrors.get(rowIndex).add(new CSVDatatypeError(rowIndex, colIndex, header, dataType.getLabel(), value));
    }

    public void addURINotFoundError(int rowIndex, int colIndex, String header, URI subjectURI, URI objectURI) {
        if (!uriNotFoundErrors.containsKey(rowIndex)) {
            uriNotFoundErrors.put(rowIndex, new ArrayList<>());
        }
        uriNotFoundErrors.get(rowIndex).add(new CSVURINotFoundError(rowIndex, colIndex, header, subjectURI, objectURI));
    }

    public void addInvalidURIError(int rowIndex, int colIndex, String header, String value) {
        if (!invalidURIErrors.containsKey(rowIndex)) {
            invalidURIErrors.put(rowIndex, new ArrayList<>());
        }
        invalidURIErrors.get(rowIndex).add(new CSVInvalidURIError(rowIndex, colIndex, header, value));
    }

    public void addMissingRequiredValue(int rowIndex, int colIndex, String header) {
                if (!missingRequiredValueErrors.containsKey(rowIndex)) {
            missingRequiredValueErrors.put(rowIndex, new ArrayList<>());
        }
        missingRequiredValueErrors.get(rowIndex).add(new CSVMissingRequiredValueError(rowIndex, colIndex, header));
    }

}
