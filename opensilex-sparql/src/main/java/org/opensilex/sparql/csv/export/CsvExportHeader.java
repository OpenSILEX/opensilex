package org.opensilex.sparql.csv.export;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CsvExportHeader {

    private Set<String> columns;
    private List<String> headerNames;

    public Set<String> getColumns() {
        return columns;
    }

    public CsvExportHeader setColumns(Set<String> header) {
        this.columns = header;
        return this;
    }

    public List<String> getHeaderNames() {
        return headerNames;
    }

    public CsvExportHeader setColumnNames(List<String> headerNames) {
        this.headerNames = headerNames;
        return this;
    }
}
