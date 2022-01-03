package org.opensilex.sparql.csv;

import org.opensilex.sparql.owl.OwlRestrictionValidator;
import org.opensilex.sparql.csv.error.CSVValidationModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;

public class CsvOwlRestrictionValidator extends OwlRestrictionValidator<CsvCellValidationContext> {

    private final CSVValidationModel validationModel;
    private final URI graph;

    public CsvOwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore, CSVValidationModel validationModel, URI graph) {
        super(sparql, ontologyStore);
        this.validationModel = validationModel;
        this.graph = graph;
    }

    @Override
    protected void addUnknownPropertyError(URI classURI, CsvCellValidationContext cellContext) {
        CSVCell cell = cellContext.getCsvCell();
        validationModel.addInvalidValueError(cell);
    }

    @Override
    protected void addInvalidValueError(String customError, CsvCellValidationContext cellContext) {
        CSVCell cell = cellContext.getCsvCell();
        validationModel.addInvalidValueError(cell);
    }

    @Override
    protected void addMissingRequiredValue(URI classURI, CsvCellValidationContext cellContext) {
        CSVCell cell = cellContext.getCsvCell();
        validationModel.addMissingRequiredValue(cell);
    }

    @Override
    protected void addInvalidDatatypeError(URI datatype, CsvCellValidationContext cellContext, String customError) {
        CSVCell cell = cellContext.getCsvCell();
//        cell.setValue(value);
//        cell.setHeader(customError != null ? customError : property.toString());
        validationModel.addInvalidDatatypeError(cell, datatype);
    }

    @Override
    protected void addInvalidURIError(CsvCellValidationContext cellContext, String customError) {
        CSVCell cell = cellContext.getCsvCell();
//        cell.setValue(value);
//        cell.setHeader(customError != null ? customError : property.toString());
        validationModel.addInvalidURIError(cell);
    }


    protected void validateCsvValue(int rowIdx, int colIdx, ClassModel classModel, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction) {
        validateModelRelation(graph, classModel, model, property, value, restriction, () -> {
            CSVCell cell = new CSVCell();
            cell.setRowIndex(rowIdx);
            cell.setColIndex(colIdx);
            return new CsvCellValidationContext(cell);
        });
    }

}
