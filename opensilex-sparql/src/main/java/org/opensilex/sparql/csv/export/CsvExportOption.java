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
    Set<String> uriColumnsAsStrings;

    //A way to have extra columns that aren't expected to be URIs
    Set<String> extraColumns;

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

    public CsvExportOption<T> setUriColumnsAsStrings(Set<String> uriColumnsAsStrings) {
        this.uriColumnsAsStrings = uriColumnsAsStrings;
        return this;
    }

    public Set<String> getExtraColumns() {
        return extraColumns;
    }

    public CsvExportOption<T> setExtraColumns(Set<String> extraColumns) {
        this.extraColumns = extraColumns;
        return this;
    }

    public Class<T> getObjectClass() {
        return objectClass;
    }

    public URI getClassURI() {
        return classURI;
    }

    public String getLang() {
        return lang;
    }

    public Set<String> getUriColumnsAsStrings() {
        return uriColumnsAsStrings;
    }

    public char getMultiValuedCellSeparator() {
        return multiValuedCellSeparator;
    }

    public CsvExportOption<T> setMultiValuedCellSeparator(char multiValuedCellSeparator) {
        this.multiValuedCellSeparator = multiValuedCellSeparator;
        return this;
    }
}
