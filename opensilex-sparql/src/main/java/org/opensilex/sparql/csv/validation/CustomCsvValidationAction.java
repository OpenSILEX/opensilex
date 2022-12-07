package org.opensilex.sparql.csv.validation;

import org.opensilex.sparql.csv.CsvOwlRestrictionValidator;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.util.function.Supplier;

@FunctionalInterface
public interface CustomCsvValidationAction<T extends SPARQLResourceModel> {

    /**
     *
     * @param model
     * @param value
     * @param validator
     * @param validationConsumer
     */
    void accept(T model, String value, CsvOwlRestrictionValidator validator, Supplier<CsvCellValidationContext> validationConsumer);
}
