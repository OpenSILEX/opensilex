package org.opensilex.faidare.api;

import org.opensilex.core.variable.api.unit.UnitCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestUnitBuilder {
    private static final String stringPrefix = "default unit ";

    private URI uri = new URI("test:default-unit-uri/");

    private String name = stringPrefix + "name";
    private String description = stringPrefix + "description";
    private String symbol = stringPrefix + "symbol";
    private String alternativeSymbol = stringPrefix + "alternative symbol";
    private List<URI> exactMatch = new ArrayList<>();
    private List<URI> closeMatch = new ArrayList<>();
    private List<URI> broadMatch = new ArrayList<>();
    private List<URI> narrowMatch = new ArrayList<>();

    public TestUnitBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestUnitBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestUnitBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TestUnitBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSymbol() {
        return symbol;
    }

    public TestUnitBuilder setSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public String getAlternativeSymbol() {
        return alternativeSymbol;
    }

    public TestUnitBuilder setAlternativeSymbol(String alternativeSymbol) {
        this.alternativeSymbol = alternativeSymbol;
        return this;
    }

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public TestUnitBuilder setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public TestUnitBuilder setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
        return this;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public TestUnitBuilder setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
        return this;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public TestUnitBuilder setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
        return this;
    }

    private final List<UnitCreationDTO> dtoList = new ArrayList<>();

    public List<UnitCreationDTO> getDTOList() {
        return dtoList;
    }

    public UnitCreationDTO createDTO() throws URISyntaxException {
        UnitCreationDTO dto = new UnitCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName());
        dto.setDescription(getDescription());
        dto.setSymbol(getSymbol());
        dto.setAlternativeSymbol(getAlternativeSymbol());
        dto.setExactMatch(getExactMatch());
        dto.setCloseMatch(getCloseMatch());
        dto.setBroadMatch(getBroadMatch());
        dto.setNarrowMatch(getNarrowMatch());

        dtoList.add(dto);
        return dto;
    }
}
