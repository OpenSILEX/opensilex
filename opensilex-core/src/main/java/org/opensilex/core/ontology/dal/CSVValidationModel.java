/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.dal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vmigot
 */
public class CSVValidationModel {

    @JsonIgnore()
    private List<SPARQLResourceModel> objects = new ArrayList<>();

    private List<URI> missingHeaders = new ArrayList<>();

    private Map<Integer, String> invalidHeaderURIs = new HashMap<>();

    private Map<Integer, List<CSVDatatypeError>> datatypeErrors = new HashMap<>();

    private Map<Integer, List<CSVURINotFoundError>> uriNotFoundErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> invalidURIErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> missingRequiredValueErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> invalidValueErrors = new HashMap<>();

    private Map<Integer, CSVCell> alreadyExistingURIErrors = new HashMap<>();

    private Map<Integer, CSVDuplicateURIError> duplicateURIErrors = new HashMap<>();

    public List<URI> getMissingHeaders() {
        return missingHeaders;
    }

    public Map<Integer, List<CSVDatatypeError>> getDatatypeErrors() {
        return datatypeErrors;
    }

    public Map<Integer, List<CSVURINotFoundError>> getUriNotFoundErrors() {
        return uriNotFoundErrors;
    }

    public Map<Integer, List<CSVCell>> getInvalidURIErrors() {
        return invalidURIErrors;
    }

    public Map<Integer, List<CSVCell>> getMissingRequiredValueErrors() {
        return missingRequiredValueErrors;
    }

    public Map<Integer, String> getInvalidHeaderURIs() {
        return invalidHeaderURIs;
    }

    public Map<Integer, List<CSVCell>> getInvalidValueErrors() {
        return invalidValueErrors;
    }

    public Map<Integer, CSVCell> getAlreadyExistingURIErrors() {
        return alreadyExistingURIErrors;
    }

    public Map<Integer, CSVDuplicateURIError> getDuplicateURIErrors() {
        return duplicateURIErrors;
    }

    public List<SPARQLResourceModel> getObjects() {
        if (hasErrors()) {
            return new ArrayList<>();
        }

        return objects;
    }

    public void setObjects(List<SPARQLResourceModel> objects) {
        this.objects = objects;
    }

    public boolean hasErrors() {
        return getMissingHeaders().size() > 0
                || getDatatypeErrors().size() > 0
                || getUriNotFoundErrors().size() > 0
                || getInvalidURIErrors().size() > 0
                || getMissingRequiredValueErrors().size() > 0
                || getInvalidHeaderURIs().size() > 0
                || getInvalidValueErrors().size() > 0
                || getAlreadyExistingURIErrors().size() > 0
                || getDuplicateURIErrors().size() > 0;
    }

    public void addMissingHeaders(Collection<URI> headers) {
        missingHeaders.addAll(headers);
    }

    void addInvalidHeaderURI(int i, String invalidURI) {
        invalidHeaderURIs.put(i, invalidURI);
    }

    public void addInvalidDatatypeError(CSVCell cell, URI dataType) {
        int rowIndex = cell.getRowIndex();
        if (!datatypeErrors.containsKey(rowIndex)) {
            datatypeErrors.put(rowIndex, new ArrayList<>());
        }
        datatypeErrors.get(rowIndex).add(new CSVDatatypeError(cell, dataType));
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
        invalidURIErrors.get(rowIndex).add(cell);
    }

    public void addMissingRequiredValue(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!missingRequiredValueErrors.containsKey(rowIndex)) {
            missingRequiredValueErrors.put(rowIndex, new ArrayList<>());
        }
        missingRequiredValueErrors.get(rowIndex).add(cell);
    }

    public void addInvalidValueError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidValueErrors.containsKey(rowIndex)) {
            invalidValueErrors.put(rowIndex, new ArrayList<>());
        }
        invalidValueErrors.get(rowIndex).add(cell);
    }

    public void addAlreadyExistingURIError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        alreadyExistingURIErrors.put(rowIndex, cell);
    }

    public void addDuplicateURIError(CSVCell cell, int previousRow) {
        int rowIndex = cell.getRowIndex();
        duplicateURIErrors.put(rowIndex, new CSVDuplicateURIError(cell, previousRow));
    }

    public void addObject(SPARQLResourceModel object) {
        objects.add(object);
    }

}
