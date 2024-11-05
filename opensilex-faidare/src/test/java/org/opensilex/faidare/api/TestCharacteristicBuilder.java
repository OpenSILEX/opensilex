package org.opensilex.faidare.api;

import org.opensilex.core.variable.api.characteristic.CharacteristicCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestCharacteristicBuilder {
    private static final String stringPrefix = "default characteristic ";

    private URI uri = new URI("test:default-characteristic-uri/");

    private String name = stringPrefix + "name";
    private String description = stringPrefix + "description";
    private List<URI> exactMatch = new ArrayList<>();
    private List<URI> closeMatch = new ArrayList<>();
    private List<URI> broadMatch = new ArrayList<>();
    private List<URI> narrowMatch = new ArrayList<>();

    public TestCharacteristicBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestCharacteristicBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestCharacteristicBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TestCharacteristicBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public TestCharacteristicBuilder setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public TestCharacteristicBuilder setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
        return this;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public TestCharacteristicBuilder setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
        return this;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public TestCharacteristicBuilder setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
        return this;
    }

    private final List<CharacteristicCreationDTO> dtoList = new ArrayList<>();

    public List<CharacteristicCreationDTO> getDTOList() {
        return dtoList;
    }

    public CharacteristicCreationDTO createDTO() throws URISyntaxException {
        CharacteristicCreationDTO dto = new CharacteristicCreationDTO();

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
