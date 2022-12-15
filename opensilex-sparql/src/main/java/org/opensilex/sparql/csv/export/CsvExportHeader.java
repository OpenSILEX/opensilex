package org.opensilex.sparql.csv.export;

import java.util.LinkedHashSet;
import java.util.List;

public class CsvExportHeader {

    private LinkedHashSet<String> columns;
    private List<String> headerNames;

    public LinkedHashSet<String> getColumns() {
        return columns;
    }

    public CsvExportHeader setColumns(LinkedHashSet<String> header) {
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
