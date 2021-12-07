/*******************************************************************************
 *                         CsvImporter.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.core.csv.dal;

import org.opensilex.core.csv.dal.error.CSVValidationModel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface CsvDao<T extends SPARQLNamedResourceModel> {

    CSVValidationModel validateCSV(URI graph,
                                   URI parentClass,
                                   InputStream file,
                                   int firstRow,
                                   String lang,
                                   Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators,
                                   List<String> customColumns);

    String exportCSV(
            List<T> objects,
            URI parentClass,
            String lang,
            BiFunction<String, T, String> customValueGenerator,
            List<String> customColumns,
            Set<String> strictlyAllowedColumns,
            Comparator<String> columnSorter
            ) throws Exception;

//    void validateCSVRow(URI graph,
//            ClassModel model,
//            String[] values,
//            int rowIndex,
//            CSVValidationModel csvValidation,
//            int uriIndex,
//            int typeIndex,
//            int nameIndex,
//            Map<String, OwlRestrictionModel> restrictionsByID,
//            Map<Integer, String> headerByIndex,
//            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
//            Map<URI, Integer> checkedURIs,
//            Map<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators);
//
//    void validateCSVSingleValue(
//            URI graph,
//            ClassModel model,
//            URI propertyURI,
//            CSVCell cell,
//            OwlRestrictionModel restriction,
//            CSVValidationModel csvValidation,
//            Map<URI, Map<URI, Boolean>> checkedClassObjectURIs,
//            BiConsumer<CSVCell, CSVValidationModel> customValidator,
//            SPARQLResourceModel object);
}
