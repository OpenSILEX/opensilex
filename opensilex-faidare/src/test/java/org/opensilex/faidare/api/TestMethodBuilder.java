package org.opensilex.faidare.api;

import org.opensilex.core.variable.api.method.MethodCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestMethodBuilder {
    private static final String stringPrefix = "default method ";

    private URI uri = new URI("test:default-method-uri/");

    private String name = stringPrefix + "name";
    private String description = stringPrefix + "description";
    private List<URI> exactMatch = new ArrayList<>();
    private List<URI> closeMatch = new ArrayList<>();
    private List<URI> broadMatch = new ArrayList<>();
    private List<URI> narrowMatch = new ArrayList<>();

    public TestMethodBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestMethodBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestMethodBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TestMethodBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public TestMethodBuilder setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public TestMethodBuilder setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
        return this;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public TestMethodBuilder setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
        return this;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public TestMethodBuilder setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
        return this;
    }

    private final List<MethodCreationDTO> dtoList = new ArrayList<>();

    public List<MethodCreationDTO> getDTOList() {
        return dtoList;
    }

    public MethodCreationDTO createDTO() throws URISyntaxException {
        MethodCreationDTO dto = new MethodCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName());
        dto.setDescription(getDescription());
        dto.setExactMatch(getExactMatch());
        dto.setCloseMatch(getCloseMatch());
        dto.setBroadMatch(getBroadMatch());
        dto.setNarrowMatch(getNarrowMatch());

        dtoList.add(dto);
        return dto;
    }
}
