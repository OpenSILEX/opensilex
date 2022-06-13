package org.opensilex.sparql.csv;

import java.net.URI;
import java.util.*;

public interface CsvImporter {

    Map<Integer, URI> readHeader(Iterator<String[]> rowIterator, CSVValidationModel csvValidationModel);

    void readBody(Iterator<String[]> rowIterator, Map<Integer, URI> columnByIndex, CsvOwlRestrictionValidator restrictionValidator, boolean validOnly) throws Exception;

    default Set<String> getCustomColumns() {
        return Collections.emptySet();
    }

}
