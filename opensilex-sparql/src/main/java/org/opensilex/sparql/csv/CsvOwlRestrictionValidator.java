package org.opensilex.sparql.csv;

import org.opensilex.sparql.csv.validation.CsvCellValidationContext;
import org.opensilex.sparql.owl.OwlRestrictionValidator;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;

/**
 * Extension of {@link OwlRestrictionValidator} which handle error which are associated to a specific CSV cell.
 * @author rcolin
 */
public class CsvOwlRestrictionValidator extends OwlRestrictionValidator<CsvCellValidationContext> {

    private final CSVValidationModel validationModel;
    private final URI graph;

    public CsvOwlRestrictionValidator(SPARQLService sparql, OntologyStore ontologyStore, URI graph, int nbErrorLimit) {
        super(sparql, ontologyStore,nbErrorLimit);
        this.validationModel = new CSVValidationModel();
        this.graph = graph;
    }

    @Override
    public void addUnknownPropertyError(CsvCellValidationContext cellContext) {
        super.addUnknownPropertyError(cellContext);
        validationModel.addInvalidValueError(cellContext);
    }

    @Override
    public void addInvalidValueError(CsvCellValidationContext cellContext) {
        super.addInvalidValueError(cellContext);
        validationModel.addInvalidValueError(cellContext);
    }

    @Override
    public void addMissingRequiredValue(CsvCellValidationContext cellContext) {
        super.addMissingRequiredValue(cellContext);
        validationModel.addMissingRequiredValue(cellContext);
    }

    @Override
    public void addInvalidDatatypeError(CsvCellValidationContext cellContext, URI datatype) {
        super.addInvalidDatatypeError(cellContext,datatype);
        validationModel.addInvalidDatatypeError(cellContext, datatype);
    }

    @Override
    public void addInvalidURIError(CsvCellValidationContext cellContext) {
        super.addInvalidURIError(cellContext);
        validationModel.addInvalidURIError(cellContext);
    }

    @Override
    public void addAlreadyExistingURIError(CsvCellValidationContext context) {
        super.addAlreadyExistingURIError(context);
        validationModel.addAlreadyExistingURIError(context);
    }

    /**
     * Register error when the row size is equals to the expected CSV header size
     * @param context error context
     */
    public void addInvalidRowSizeError(CsvCellValidationContext context){
        nbError++;
        validationModel.addAInvalidRowSizeError(context);
    }

    protected void validateCsvValue(int rowIdx, int colIdx, ClassModel classModel, SPARQLResourceModel model, String value, URI property, OwlRestrictionModel restriction) {
        validateModelRelation(graph, classModel, model, property, value, restriction, () -> {
            CsvCellValidationContext cell = new CsvCellValidationContext();
            cell.setRowIndex(rowIdx+AbstractCsvImporter.CSV_HEADER_HUMAN_READABLE_ROW_OFFSET);
            cell.setColIndex(colIdx+AbstractCsvImporter.CSV_HEADER_HUMAN_READABLE_COLUMN_OFFSET);
            return cell;
        });
    }

    public CSVValidationModel getValidationModel() {
        return validationModel;
    }
}
