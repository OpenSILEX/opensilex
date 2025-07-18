package org.opensilex.core.data.bll.dataImport;

import org.opensilex.core.annotation.dal.AnnotationModel;
import org.opensilex.core.data.dal.DAOContext;
import org.opensilex.core.data.dal.DataCSVValidationModel;
import org.opensilex.core.data.utils.ParsedDateTimeMongo;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.*;

public class ValidationContext {
    private final ProvenanceModel provenance;
    private final String[] values;
    private final int rowIndex;
    private final Map<Integer, String> headerByIndex;
    private final ExperimentContext experimentContext;
    private final TargetContext targetContext;
    private final DeviceContext deviceContext;
    private final DAOContext daoContext;
    private final DataCSVValidationModel localCsvValidation;
    private final DataCSVValidationModel csvValidation;

    private boolean validRow = true;
    private boolean missingTargetOrDevice = false;
    private boolean sensingDeviceFoundFromProvenance = false;

    private List<URI> experiments = new ArrayList<>();
    private final Set<Integer> colsToDoAtEnd = new HashSet<>();


    private ParsedDateTimeMongo parsedDateTimeMongo;
    private AnnotationModel annotationFromAnnotationColumn;
    private DeviceModel deviceFromDeviceColumn;
    private SPARQLNamedResourceModel target;
    private SPARQLNamedResourceModel object;

    private int targetColIndex = 0;
    private int deviceColIndex = 0;
    private int annotationIndex = 0;


    public ValidationContext(
            ProvenanceModel provenance,
            String[] values,
            int rowIndex,
            Map<Integer, String> headerByIndex,
            ExperimentContext experimentContext,
            TargetContext targetContext,
            DeviceContext deviceContext,
            DAOContext daoContext,
            DataCSVValidationModel localCsvValidation,
            DataCSVValidationModel csvValidation,
            boolean sensingDeviceFoundFromProvenance
    ) {
        this.provenance = provenance;
        this.values = values;
        this.rowIndex = rowIndex;
        this.headerByIndex = headerByIndex;
        this.experimentContext = experimentContext;
        this.targetContext = targetContext;
        this.deviceContext = deviceContext;
        this.daoContext = daoContext;
        this.localCsvValidation = localCsvValidation;
        this.csvValidation = csvValidation;
        this.sensingDeviceFoundFromProvenance = sensingDeviceFoundFromProvenance;
    }

    public ProvenanceModel getProvenance() {
        return provenance;
    }

    public String[] getValues() {
        return values;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public Map<Integer, String> getHeaderByIndex() {
        return headerByIndex;
    }

    public ExperimentContext getExperimentContext() {
        return experimentContext;
    }

    public TargetContext getTargetContext() {
        return targetContext;
    }

    public DeviceContext getDeviceContext() {
        return deviceContext;
    }

    public DAOContext getDaoContext() {
        return daoContext;
    }

    public DataCSVValidationModel getLocalCsvValidation() {
        return localCsvValidation;
    }

    public DataCSVValidationModel getCsvValidation() {
        return csvValidation;
    }

    public boolean isValidRow() {
        return validRow;
    }

    public void setValidRow(boolean validRow) {
        this.validRow = validRow;
    }

    public boolean isMissingTargetOrDevice() {
        return missingTargetOrDevice;
    }

    public void setMissingTargetOrDevice(boolean missingTargetOrDevice) {
        this.missingTargetOrDevice = missingTargetOrDevice;
    }

    public List<URI> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<URI> experiments) {
        this.experiments = experiments;
    }

    public Set<Integer> getColsToDoAtEnd() {
        return colsToDoAtEnd;
    }

    public ParsedDateTimeMongo getParsedDateTimeMongo() {
        return parsedDateTimeMongo;
    }

    public void setParsedDateTimeMongo(ParsedDateTimeMongo parsedDateTimeMongo) {
        this.parsedDateTimeMongo = parsedDateTimeMongo;
    }

    public AnnotationModel getAnnotationFromAnnotationColumn() {
        return annotationFromAnnotationColumn;
    }

    public void setAnnotationFromAnnotationColumn(AnnotationModel annotationFromAnnotationColumn) {
        this.annotationFromAnnotationColumn = annotationFromAnnotationColumn;
    }

    public DeviceModel getDeviceFromDeviceColumn() {
        return deviceFromDeviceColumn;
    }

    public void setDeviceFromDeviceColumn(DeviceModel deviceFromDeviceColumn) {
        this.deviceFromDeviceColumn = deviceFromDeviceColumn;
    }

    public SPARQLNamedResourceModel getTarget() {
        return target;
    }

    public void setTarget(SPARQLNamedResourceModel target) {
        this.target = target;
    }

    public SPARQLNamedResourceModel getObject() {
        return object;
    }

    public void setObject(SPARQLNamedResourceModel object) {
        this.object = object;
    }

    public int getTargetColIndex() {
        return targetColIndex;
    }

    public void setTargetColIndex(int targetColIndex) {
        this.targetColIndex = targetColIndex;
    }

    public int getDeviceColIndex() {
        return deviceColIndex;
    }

    public void setDeviceColIndex(int deviceColIndex) {
        this.deviceColIndex = deviceColIndex;
    }

    public int getAnnotationIndex() {
        return annotationIndex;
    }

    public void setAnnotationIndex(int annotationIndex) {
        this.annotationIndex = annotationIndex;
    }

    public boolean isSensingDeviceFoundFromProvenance() {
        return sensingDeviceFoundFromProvenance;
    }
}