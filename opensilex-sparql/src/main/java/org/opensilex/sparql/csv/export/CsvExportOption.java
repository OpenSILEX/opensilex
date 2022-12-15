package org.opensilex.sparql.csv.export;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.List;
import java.util.Set;

public class CsvExportOption<T extends SPARQLResourceModel> {

    List<T> results;
    Class<T> objectClass;

    URI classURI;
    String lang;
    Set<String> columns;

    char multiValuedCellSeparator;

    public CsvExportOption(){
        lang = OpenSilex.DEFAULT_LANGUAGE;
        multiValuedCellSeparator = ' ';
    }

    public List<T> getResults() {
        return results;
    }

    public CsvExportOption<T> setResults(List<T> results) {
        this.results = results;
        return this;
    }

    public CsvExportOption<T> setObjectClass(Class<T> objectClass) {
        this.objectClass = objectClass;
        return this;
    }

    public CsvExportOption<T> setClassURI(URI classURI) {
        this.classURI = classURI;
        return this;
    }

    public CsvExportOption<T> setLang(String lang) {
        this.lang = lang;
        return this;
    }

    public CsvExportOption<T> setColumns(Set<String> columns) {
        this.columns = columns;
        return this;
    }

    public CsvExportOption<T> setMultiValuedCellSeparator(char multiValuedCellSeparator) {
        this.multiValuedCellSeparator = multiValuedCellSeparator;
        return this;
    }
}
