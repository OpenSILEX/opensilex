/*******************************************************************************
 *                         CsvImporter.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.core.csv.dal;

import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface CsvDao<T extends SPARQLNamedResourceModel> {

    String exportCSV(
            List<T> objects,
            URI parentClass,
            String lang,
            BiFunction<String, T, String> customValueGenerator,
            List<String> customColumns,
            Set<String> strictlyAllowedColumns,
            Comparator<String> columnSorter
            ) throws IOException, SPARQLException;

}
