/*******************************************************************************
 *                         CSVValidationModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.csv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensilex.sparql.csv.error.CSVDatatypeError;
import org.opensilex.sparql.csv.error.CSVDuplicateURIError;
import org.opensilex.sparql.csv.error.CSVURINotFoundError;
import org.opensilex.sparql.csv.header.CsvHeader;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * @author vmigot
 */
public class CSVValidationModel {

    @JsonIgnore()
    private List<SPARQLResourceModel> objects = new ArrayList<>();

    @JsonIgnore()
    private Map<String, Object> objectsMetadata = new HashMap<>();

    @JsonIgnore()
    private Map<String, List<URI>> uriByNames = new HashMap<>();

    private List<String> missingHeaders = new ArrayList<>();
    
    private Set<Integer> emptyHeaders = new HashSet<>();

    private Map<Integer, String> invalidHeaderURIs = new HashMap<>();

    private Map<Integer, List<CSVDatatypeError>> datatypeErrors = new HashMap<>();

    private Map<Integer, List<CSVURINotFoundError>> uriNotFoundErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> invalidURIErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> missingRequiredValueErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> invalidValueErrors = new HashMap<>();

    private Map<Integer, List<CSVCell>> alreadyExistingURIErrors = new HashMap<>();

    private Map<Integer, List<CSVDuplicateURIError>> duplicateURIErrors = new HashMap<>();

    private Map<Integer,List<CSVCell>> invalidRowSizeErrors;

    private int nbObjectImported;

    private String validationToken;

    private CsvHeader csvHeader;

    public List<String> getMissingHeaders() {
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

    public Map<Integer, List<CSVCell>> getAlreadyExistingURIErrors() {
        return alreadyExistingURIErrors;
    }

    public Map<Integer, List<CSVDuplicateURIError>> getDuplicateURIErrors() {
        return duplicateURIErrors;
    }

    public CSVValidationModel(){
        this.invalidRowSizeErrors = new HashMap<>(1);

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

    public Map<String, Object> getObjectsMetadata() {
        if (hasErrors()) {
            return new HashMap<>();
        }

        return objectsMetadata;
    }

    public void setObjectsMetadata(Map<String, Object> objectsMetadata) {
        this.objectsMetadata = objectsMetadata;
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
                || getDuplicateURIErrors().size() > 0
                || getEmptyHeaders().size() > 0
                || ! invalidRowSizeErrors.isEmpty();
    }

    public void addMissingHeaders(Collection<String> headers) {
        missingHeaders.addAll(headers);
    }

    public void addInvalidHeaderURI(int i, String invalidURI) {
        invalidHeaderURIs.put(i, invalidURI);
    }

    public void addInvalidDatatypeError(CSVCell cell, URI dataType) {
        datatypeErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(new CSVDatatypeError(cell, dataType));
    }

    public void addURINotFoundError(CSVCell cell, URI subjectURI, URI objectURI) {
        uriNotFoundErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(new CSVURINotFoundError(cell, subjectURI, objectURI));
    }

    public void addInvalidURIError(CSVCell cell) {
        invalidURIErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(cell);
    }

    public void addMissingRequiredValue(CSVCell cell) {
        missingRequiredValueErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(cell);
    }


    public void addInvalidValueError(CSVCell cell) {
        invalidValueErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(cell);
    }

    public void addAlreadyExistingURIError(CSVCell cell) {
        alreadyExistingURIErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(cell);
    }

    public void addDuplicateURIError(CSVCell cell, int previousRow) {
        duplicateURIErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(new CSVDuplicateURIError(cell, previousRow));
    }

    public void addObject(String name, SPARQLNamedResourceModel object) {
        if (name != null) {
            if (!uriByNames.containsKey(name)) {
                uriByNames.put(name, new ArrayList<>());
            }
            uriByNames.get(name).add(object.getUri());
        }
        objects.add(object);
    }
    
    public List<URI> getObjectNameUris(String name) {
        return uriByNames.get(name);
    }

    public boolean containsObject(SPARQLResourceModel object) {
        return objects.contains(object);
    }

    public void addObjectMetadata(String metadataKey, Object value) {
        objectsMetadata.put(metadataKey, value);
    }

    public Set<Integer> getEmptyHeaders() {
        return emptyHeaders;
    }

    public void addEmptyHeader(Integer emptyHeader) {
        emptyHeaders.add(emptyHeader);
    }

    public int getNbObjectImported() {
        return nbObjectImported;
    }

    public void setNbObjectImported(int nbObjectImported) {
        this.nbObjectImported = nbObjectImported;
    }

    public String getValidationToken() {
        return validationToken;
    }

    public CSVValidationModel setValidationToken(String validationToken) {
        this.validationToken = validationToken;
        return this;
    }

    public CsvHeader getCsvHeader() {
        return csvHeader;
    }

    public CSVValidationModel setCsvHeader(CsvHeader csvHeader) {
        this.csvHeader = csvHeader;
        return this;
    }

    public Map<Integer, List<CSVCell>> getInvalidRowSizeErrors() {
        return invalidRowSizeErrors;
    }

    public void addAInvalidRowSizeError(CSVCell cell) {
        invalidRowSizeErrors
                .computeIfAbsent(cell.getRowIndex(), rowIndex -> new ArrayList<>())
                .add(cell);
    }

}
