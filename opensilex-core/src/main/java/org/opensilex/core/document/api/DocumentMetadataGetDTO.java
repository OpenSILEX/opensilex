package org.opensilex.core.document.api;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

public class DocumentMetadataGetDTO {

    private Map<URI, Map<String, Object>> variables;
    private List<String> keywords;
    private LocalDate firstElementDate;
    private LocalDate lastElementDate;
    private String name;

    public DocumentMetadataGetDTO(String name, Map<URI, Map<String, Object>> variables, List<String> keywords, LocalDate firstElementDate, LocalDate lastElementDate) {
        this.name = name;
        this.variables = variables;
        this.keywords = keywords;
        this.firstElementDate = firstElementDate;
        this.lastElementDate = lastElementDate;
    }

    // Getters and setters for the fields
    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public Map<URI, Map<String, Object>> getVariables() {
        return variables;
    }

    public void setVariables(Map<URI, Map<String, Object>> variables) {
        this.variables = variables;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public LocalDate getFirstElementDate() {
        return firstElementDate;
    }

    public DocumentMetadataGetDTO setFirstElementDate(LocalDate firstElementDate) {
        this.firstElementDate = firstElementDate;
        return null;
    }

    public LocalDate getLastElementDate() {
        return lastElementDate;
    }

    public DocumentMetadataGetDTO setLastElementDate(LocalDate lastElementDate) {
        this.lastElementDate = lastElementDate;
        return null;
    }
}
