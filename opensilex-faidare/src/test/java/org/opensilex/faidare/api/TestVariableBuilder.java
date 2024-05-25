package org.opensilex.faidare.api;

import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.variable.api.VariableCreationDTO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class TestVariableBuilder {
    private static final String stringPrefix = "default variable ";

    private URI uri = new URI("test:default-variable-uri/");

    private String name = stringPrefix + "name";
    private String alternativeName = stringPrefix + "alternative name";
    private String description = stringPrefix + "description";
    private URI entity;
    private URI entityOfInterest;
    private URI characteristic;
    private URI method;
    private URI unit;
    private URI trait = new URI("test:default-trait-uri/");
    private String traitName = stringPrefix + "trait name";
    private URI dataType = new URI(XSD.integer.getURI());
    private String timeInterval;
    private String samplingInterval;
    private List<URI> species = new ArrayList<>();
    private List<URI> exactMatch = new ArrayList<>();
    private List<URI> closeMatch = new ArrayList<>();
    private List<URI> broadMatch = new ArrayList<>();
    private List<URI> narrowMatch = new ArrayList<>();

    public TestVariableBuilder() throws URISyntaxException {
    }

    public URI getUri() {
        return uri;
    }

    public TestVariableBuilder setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestVariableBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getAlternativeName() {
        return alternativeName;
    }

    public TestVariableBuilder setAlternativeName(String alternativeName) {
        this.alternativeName = alternativeName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public TestVariableBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public URI getEntity() {
        return entity;
    }

    public TestVariableBuilder setEntity(URI entity) {
        this.entity = entity;
        return this;
    }

    public URI getEntityOfInterest() {
        return entityOfInterest;
    }

    public TestVariableBuilder setEntityOfInterest(URI entityOfInterest) {
        this.entityOfInterest = entityOfInterest;
        return this;
    }

    public URI getCharacteristic() {
        return characteristic;
    }

    public TestVariableBuilder setCharacteristic(URI characteristic) {
        this.characteristic = characteristic;
        return this;
    }

    public URI getMethod() {
        return method;
    }

    public TestVariableBuilder setMethod(URI method) {
        this.method = method;
        return this;
    }

    public URI getUnit() {
        return unit;
    }

    public TestVariableBuilder setUnit(URI unit) {
        this.unit = unit;
        return this;
    }

    public URI getTrait() {
        return trait;
    }

    public TestVariableBuilder setTrait(URI trait) {
        this.trait = trait;
        return this;
    }

    public String getTraitName() {
        return traitName;
    }

    public TestVariableBuilder setTraitName(String traitName) {
        this.traitName = traitName;
        return this;
    }

    public URI getDataType() {
        return dataType;
    }

    public TestVariableBuilder setDataType(URI dataType) {
        this.dataType = dataType;
        return this;
    }

    public String getTimeInterval() {
        return timeInterval;
    }

    public TestVariableBuilder setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
        return this;
    }

    public String getSamplingInterval() {
        return samplingInterval;
    }

    public TestVariableBuilder setSamplingInterval(String samplingInterval) {
        this.samplingInterval = samplingInterval;
        return this;
    }

    public List<URI> getSpecies() {
        return species;
    }

    public TestVariableBuilder setSpecies(List<URI> species) {
        this.species = species;
        return this;
    }

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public TestVariableBuilder setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
        return this;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public TestVariableBuilder setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
        return this;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public TestVariableBuilder setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
        return this;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public TestVariableBuilder setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
        return this;
    }

    private final List<VariableCreationDTO> dtoList = new ArrayList<>();

    public List<VariableCreationDTO> getDTOList() {
        return dtoList;
    }

    public VariableCreationDTO createDTO() throws URISyntaxException {
        VariableCreationDTO dto = new VariableCreationDTO();

        dto.setUri(new URI(getUri().toString() + dtoList.size()));
        dto.setName(getName());
        dto.setAlternativeName(getAlternativeName());
        dto.setDescription(getDescription());
        dto.setEntity(getEntity());
        dto.setEntityOfInterest(getEntityOfInterest());
        dto.setCharacteristic(getCharacteristic());
        dto.setMethod(getMethod());
        dto.setUnit(getUnit());
        dto.setTrait(new URI(getTrait().toString() + dtoList.size()));
        dto.setTraitName(getTraitName());
        dto.setDataType(getDataType());
        dto.setTimeInterval(getTimeInterval());
        dto.setSamplingInterval(getSamplingInterval());
        dto.setSpecies(getSpecies());
        dto.setExactMatch(getExactMatch());
        dto.setCloseMatch(getCloseMatch());
        dto.setBroadMatch(getBroadMatch());
        dto.setNarrowMatch(getNarrowMatch());

        dtoList.add(dto);
        return dto;
    }
}
