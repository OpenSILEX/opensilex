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

    private List<URI> missingHeaders = new ArrayList<>();
    
    private Map<Integer, String> invalidHeaderURIs = new HashMap<>();

    private Map<Integer, List<CSVDatatypeError>> datatypeErrors = new HashMap<>();

    private Map<Integer, List<CSVURINotFoundError>> uriNotFoundErrors = new HashMap<>();

    private Map<Integer, List<CSVInvalidURIError>> invalidURIErrors = new HashMap<>();

    private Map<Integer, List<CSVMissingRequiredValueError>> missingRequiredValueErrors = new HashMap<>();

    private Map<Integer, List<CSVInvalidValueError>> invalidValueErrors = new HashMap<>();

    public List<URI> getMissingHeaders() {
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

    public Map<Integer, String> getInvalidHeaderURIs() {
        return invalidHeaderURIs;
    }

    public Map<Integer, List<CSVInvalidValueError>> getInvalidValueErrors() {
        return invalidValueErrors;
    }
    
    public boolean hasErrors() {
        return getMissingHeaders().size() > 0
                || getDatatypeErrors().size() > 0
                || getUriNotFoundErrors().size() > 0
                || getInvalidURIErrors().size() > 0
                || getMissingRequiredValueErrors().size() > 0
                || getInvalidHeaderURIs().size() > 0
                || getInvalidValueErrors().size() > 0;
    }

    public void addMissingHeaders(Collection<URI> headers) {
        missingHeaders.addAll(headers);
    }
    
    void addInvalidHeaderURI(int i, String invalidURI) {
        invalidHeaderURIs.put(i, invalidURI);
    }

    public void addInvalidDatatypeError(CSVCell cell, BuiltInDatatypes dataType) {
        int rowIndex = cell.getRowIndex();
        if (!datatypeErrors.containsKey(rowIndex)) {
            datatypeErrors.put(rowIndex, new ArrayList<>());
        }
        datatypeErrors.get(rowIndex).add(new CSVDatatypeError(cell, dataType.getLabel()));
    }

    public void addURINotFoundError(CSVCell cell, URI subjectURI, URI objectURI) {
        int rowIndex = cell.getRowIndex();
        if (!uriNotFoundErrors.containsKey(rowIndex)) {
            uriNotFoundErrors.put(rowIndex, new ArrayList<>());
        }
        uriNotFoundErrors.get(rowIndex).add(new CSVURINotFoundError(cell, subjectURI, objectURI));
    }

    public void addInvalidURIError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidURIErrors.containsKey(rowIndex)) {
            invalidURIErrors.put(rowIndex, new ArrayList<>());
        }
        invalidURIErrors.get(rowIndex).add(new CSVInvalidURIError(cell));
    }

    public void addMissingRequiredValue(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!missingRequiredValueErrors.containsKey(rowIndex)) {
            missingRequiredValueErrors.put(rowIndex, new ArrayList<>());
        }
        missingRequiredValueErrors.get(rowIndex).add(new CSVMissingRequiredValueError(cell));
    }

    public void addInvalidValueError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidValueErrors.containsKey(rowIndex)) {
            invalidValueErrors.put(rowIndex, new ArrayList<>());
        }
        invalidValueErrors.get(rowIndex).add(new CSVInvalidValueError(cell));
    }

}
