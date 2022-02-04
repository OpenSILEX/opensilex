//******************************************************************************
//                          CodeLotCSVValidationModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.dal;

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
 * @author Maximilian Hart TODO NOt done!
 */
public class CodeLotCSVValidationModel extends CSVValidationModel {

    //no clue what this is
    //@JsonIgnore()
    //private HashMap<DataModel, Integer> data = new HashMap<>();
    @JsonIgnore()
    private HashMap<CodeLotModel, Integer> unsavableData = new HashMap<>();

    private final Map<Integer, List<CSVCell>> noParentErrors = new HashMap<>();
    private final Map<Integer, List<CSVCell>> boucleErrors = new HashMap<>();
    private final Map<Integer, List<CSVCell>> linkedSibblingErrors = new HashMap<>();
    private final Map<Integer, List<CSVCell>> shortCircuitErrors = new HashMap<>();
    private Map<Integer, List<CSVCell>> duplicatedDataErrors = new HashMap<>();
    private Map<Integer, List<CSVCell>> formTypeErrors = new HashMap<>();

    private List<String> headers = new ArrayList<>();

    private List<String> headersLabels = new ArrayList<>();

    private Integer nbLinesImported = 0;

    private Integer nbLinesToImport = 0;

    private boolean validationStep = false;

    private boolean tooLargeDataset = false;

    private boolean insertionStep = false;

    private boolean validCSV = false;

    //private boolean tooLargeDataset = false;
    private String errorMessage;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public void setHeadersFromArray(String[] headers) {
        if (headers != null) {
            this.headers = Arrays.asList(headers);
        }
    }

    public List<String> getHeadersLabels() {
        return headersLabels;
    }

    public void setHeadersLabel(List<String> headers) {
        this.headersLabels = headers;
    }

    public void setHeadersLabelsFromArray(String[] headers) {
        if (headers != null) {
            this.headersLabels = Arrays.asList(headers);
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<Integer, List<CSVCell>> getDuplicatedDataErrors() {
        return duplicatedDataErrors;
    }

    public void setDuplicatedDataErrors(Map<Integer, List<CSVCell>> duplicatedDataErrors) {
        this.duplicatedDataErrors = duplicatedDataErrors;
    }

    public HashMap<CodeLotModel, Integer> getUnsavableData() {
        return unsavableData;
    }

    public void setUnsavableData(HashMap<CodeLotModel, Integer> codes) {
        this.unsavableData = codes;
    }

    public void addUnsavableData(CodeLotModel code, Integer rowNumber) {
        this.unsavableData.put(code, rowNumber);
    }

    public boolean isTooLargeDataset() {
        return tooLargeDataset;
    }

    public void setTooLargeDataset(boolean tooLargeDataset) {
        this.tooLargeDataset = tooLargeDataset;
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

    @Override
    public boolean hasErrors() {
        return super.hasErrors()
                || noParentErrors.size() > 0
                || tooLargeDataset
                || linkedSibblingErrors.size() > 0
                || shortCircuitErrors.size() > 0
                || boucleErrors.size() > 0;
    }

    //TODO add and get the errors ive defined
    public void addNoParentError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!noParentErrors.containsKey(rowIndex)) {
            noParentErrors.put(rowIndex, new ArrayList<>());
        }
        noParentErrors.get(rowIndex).add(cell);
    }

    public Map<Integer, List<CSVCell>> getNoParentErrors() {
        return noParentErrors;
    }

    public void addBoucleError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!boucleErrors.containsKey(rowIndex)) {
            boucleErrors.put(rowIndex, new ArrayList<>());
        }
        boucleErrors.get(rowIndex).add(cell);
    }

    public Map<Integer, List<CSVCell>> getTypeErrors() {
        return formTypeErrors;
    }

    public void addTypeError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!formTypeErrors.containsKey(rowIndex)) {
            formTypeErrors.put(rowIndex, new ArrayList<>());
        }
        formTypeErrors.get(rowIndex).add(cell);
    }

    public void addDuplicatedDataError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!duplicatedDataErrors.containsKey(rowIndex)) {
            duplicatedDataErrors.put(rowIndex, new ArrayList<>());
        }
        duplicatedDataErrors.get(rowIndex).add(cell);
    }

    public Map<Integer, List<CSVCell>> getBoucleErrors() {
        return boucleErrors;
    }

    public void addLinkedSibblingError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!linkedSibblingErrors.containsKey(rowIndex)) {
            linkedSibblingErrors.put(rowIndex, new ArrayList<>());
        }
        linkedSibblingErrors.get(rowIndex).add(cell);
    }

    public Map<Integer, List<CSVCell>> getLinkedSibblingErrors() {
        return linkedSibblingErrors;
    }

    public void addShortCircuitError(CSVCell cell) {
        int rowIndex = cell.getRowIndex();
        if (!shortCircuitErrors.containsKey(rowIndex)) {
            shortCircuitErrors.put(rowIndex, new ArrayList<>());
        }
        shortCircuitErrors.get(rowIndex).add(cell);
    }

    public Map<Integer, List<CSVCell>> getShortCircuitErrors() {
        return shortCircuitErrors;
    }

}
