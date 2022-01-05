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

    private final CSVValidationModel csvValidationModel;
    private final URI graph;

    public CsvOwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore, CSVValidationModel validationModel, URI graph) {
        super(sparql, ontologyStore);
        this.csvValidationModel = validationModel;
        this.graph = graph;
    }

    @Override
    protected void addUnknownPropertyError(CsvCellValidationContext cellContext) {
        validationModel.addInvalidValueError(cellContext.getCsvCell());
    }

    @Override
    protected void addInvalidValueError(CsvCellValidationContext cellContext) {
        validationModel.addInvalidValueError(cellContext.getCsvCell());
    }

    @Override
    protected void addMissingRequiredValue(CsvCellValidationContext cellContext) {
        validationModel.addMissingRequiredValue(cellContext.getCsvCell());
    }

    @Override
    protected void addInvalidDatatypeError(CsvCellValidationContext cellContext, URI datatype) {
        validationModel.addInvalidDatatypeError(cellContext.getCsvCell(), datatype);
    }

    @Override
    protected void addInvalidURIError(CsvCellValidationContext cellContext) {
        validationModel.addInvalidURIError(cellContext.getCsvCell());
    }


    protected void validateCsvValue(int rowIdx, int colIdx, ClassModel classModel, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction) {
        validateModelRelation(graph, classModel, model, property, value, restriction, () -> {
            CSVCell cell = new CSVCell();
            cell.setRowIndex(rowIdx);
            cell.setColIndex(colIdx);
            return new CsvCellValidationContext(cell);
        });
    }

    public CSVValidationModel getCsvValidationModel() {
        return csvValidationModel;
    }
}
