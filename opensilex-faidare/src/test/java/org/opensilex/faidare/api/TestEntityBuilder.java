package org.opensilex.faidare.api;

import org.opensilex.core.variable.api.entity.EntityCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestEntityBuilder {
    private static final String stringPrefix = "default entity ";

    private URI uri = new URI("test:default-entity-uri/");

    private String name = stringPrefix + "name";
    private String description = stringPrefix + "description";
    private List<URI> exactMatch = new ArrayList<>();
    private List<URI> closeMatch = new ArrayList<>();
    private List<URI> broadMatch = new ArrayList<>();
    private List<URI> narrowMatch = new ArrayList<>();

    public TestEntityBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestEntityBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestEntityBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TestEntityBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public TestEntityBuilder setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public TestEntityBuilder setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
        return this;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public TestEntityBuilder setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
        return this;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public TestEntityBuilder setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
        return this;
    }

    private final List<EntityCreationDTO> dtoList = new ArrayList<>();

    public List<EntityCreationDTO> getDTOList() {
        return dtoList;
    }

    public EntityCreationDTO createDTO() throws URISyntaxException {
        EntityCreationDTO dto = new EntityCreationDTO();

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
