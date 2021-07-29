/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.CSVValidationModel;

/**
 *
 * @author vmigot
 */
public class DataCSVValidationModel extends CSVValidationModel{

    @JsonIgnore()
    private HashMap<DataModel, Integer> data = new HashMap<>();
    
    private Map<Integer, List<CSVCell>> invalidObjectErrors = new HashMap<>();
    private Map<Integer, List<CSVCell>> invalidDateErrors = new HashMap<>();
    private Map<Integer, List<CSVCell>> invalidDataTypeErrors = new HashMap<>();
    private Map<Integer, List<CSVCell>> invalidExperimentErrors = new HashMap<>();
    private Map<Integer, List<CSVCell>> invalidDeviceErrors = new HashMap<>();
    
    private Map<Integer, List<CSVCell>> duplicatedDataErrors = new HashMap<>();

    private List<String> headers = new ArrayList<>();
       
    private List<String> headersLabels = new ArrayList<>();


    private Integer nbLinesImported = 0;

    private Integer nbLinesToImport = 0;

    private boolean validationStep = false;

    private boolean insertionStep = false;

    private boolean validCSV = false;

    private boolean tooLargeDataset = false;
    
    private String errorMessage;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

   
     public void setHeadersFromArray(String[] headers) {
        if(headers != null ){
            this.headers =  Arrays.asList(headers);
        }
    }
    
     public List<String> getHeadersLabels() {
        return headersLabels;
    }

    public void setHeadersLabel(List<String> headers) {
        this.headersLabels = headers;
    }

   
     public void setHeadersLabelsFromArray(String[] headers) {
        if(headers != null ){
            this.headersLabels =  Arrays.asList(headers);
        }
    }
    

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public HashMap<DataModel, Integer> getData() {
        return data;
    }

    public void setData(HashMap<DataModel, Integer> data) {
        this.data = data;
    }
    
    
    
    public void addData(DataModel data, Integer rowNumber){
        this.data.put(data, rowNumber);
    }

    public void addDuplicatedDataError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!duplicatedDataErrors.containsKey(rowIndex)) {
            duplicatedDataErrors.put(rowIndex, new ArrayList<>());
        }
        duplicatedDataErrors.get(rowIndex).add(cell);
    }
    
   
    public Map<Integer, List<CSVCell>> getDuplicatedDataErrors() {
        return duplicatedDataErrors;
    }

    public void setDuplicatedDataErrors(Map<Integer, List<CSVCell>> duplicatedDataErrors) {
        this.duplicatedDataErrors = duplicatedDataErrors;
    }
    
    
    public Integer getNbLinesImported() {
        return nbLinesImported;
    }

    public void setNbLinesImported(Integer nbLinesImported) {
        this.nbLinesImported = nbLinesImported;
    }

    public Integer getNbLinesToImport() {
        return nbLinesToImport;
    }

    public void setNbLinesToImport(Integer nbLinesToImport) {
        this.nbLinesToImport = nbLinesToImport;
    }

    public boolean isValidationStep() {
        return validationStep;
    }

    public void setValidationStep(boolean validationStep) {
        this.validationStep = validationStep;
    }

    public boolean isInsertionStep() {
        return insertionStep;
    }

    public void setInsertionStep(boolean insertionStep) {
        this.insertionStep = insertionStep;
    }

    public boolean isValidCSV() {
        return validCSV;
    }

    public void setValidCSV(boolean validCSV) {
        this.validCSV = validCSV;
    }
 
    public boolean isTooLargeDataset() {
        return tooLargeDataset;
    }

    public void setTooLargeDataset(boolean tooLargeDataset) {
        this.tooLargeDataset = tooLargeDataset;
    }
 
    @Override
    public boolean hasErrors() {
        return super.hasErrors() 
                || tooLargeDataset
                || duplicatedDataErrors.size() > 0
                || invalidObjectErrors.size() > 0
                || invalidDateErrors.size() > 0
                || invalidDataTypeErrors.size() > 0
                ;
    }

    public Map<Integer, List<CSVCell>> getInvalidObjectErrors() {
        return invalidObjectErrors;
    }

    
     public void addInvalidObjectError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidObjectErrors.containsKey(rowIndex)) {
            invalidObjectErrors.put(rowIndex, new ArrayList<>());
        }
        invalidObjectErrors.get(rowIndex).add(cell);
    }
    
    public void setInvalidObjectErrors(Map<Integer, List<CSVCell>> invalidObjectErrors) {
        this.invalidObjectErrors = invalidObjectErrors;
    }

    public Map<Integer, List<CSVCell>> getInvalidDateErrors() {
        return invalidDateErrors;
    }

    public void setInvalidDateErrors(Map<Integer, List<CSVCell>> invalidDateErrors) {
        this.invalidDateErrors = invalidDateErrors;
    }
    
    public void addInvalidDateError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidDateErrors.containsKey(rowIndex)) {
            invalidDateErrors.put(rowIndex, new ArrayList<>());
        }
        invalidDateErrors.get(rowIndex).add(cell);
    }
    
    public void addInvalidDataTypeError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidDataTypeErrors.containsKey(rowIndex)) {
            invalidDataTypeErrors.put(rowIndex, new ArrayList<>());
        }
        invalidDataTypeErrors.get(rowIndex).add(cell);
    }

    public Map<Integer, List<CSVCell>> getInvalidDataTypeErrors() {
        return invalidDataTypeErrors;
    }

    public void setInvalidDataTypeErrors(Map<Integer, List<CSVCell>> invalidDataTypeErrors) {
        this.invalidDataTypeErrors = invalidDataTypeErrors;
    }
    
     public Map<Integer, List<CSVCell>> getInvalidExperimentErrors() {
        return invalidExperimentErrors;
    }

    
     public void addInvalidExperimentError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidExperimentErrors.containsKey(rowIndex)) {
            invalidExperimentErrors.put(rowIndex, new ArrayList<>());
        }
        invalidExperimentErrors.get(rowIndex).add(cell);
    }
    
    public void setInvalidExperimentErrors(Map<Integer, List<CSVCell>> invalidExperimentErrors) {
        this.invalidExperimentErrors = invalidExperimentErrors;
    }
    
     public Map<Integer, List<CSVCell>> getInvalidDeviceErrors() {
        return invalidDeviceErrors;
    }

    
     public void addInvalidDeviceError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!invalidDeviceErrors.containsKey(rowIndex)) {
            invalidDeviceErrors.put(rowIndex, new ArrayList<>());
        }
        invalidDeviceErrors.get(rowIndex).add(cell);
    }
    
    public void setInvalidDeviceErrors(Map<Integer, List<CSVCell>> invalidDeviceErrors) {
        this.invalidDeviceErrors = invalidDeviceErrors;
    }
    
}
