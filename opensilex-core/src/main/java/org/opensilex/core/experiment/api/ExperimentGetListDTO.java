//******************************************************************************
//                          ExperimentGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 *
 * A basic GetDTO which extends the {@link ExperimentDTO} and which add the conversion from an {@link ExperimentModel} to a {@link ExperimentGetDTO}
 *
 * @author Vincent MIGOT
 * @author Renaud COLIN
 * @author Julien BONNEFONT
 */
public class ExperimentGetListDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("start_date")
    protected LocalDate startDate;

    @JsonProperty("end_date")
    protected LocalDate endDate;

    @JsonProperty("description")
    protected String description;
    
    @JsonProperty("objective")
    protected String objective;

    @JsonProperty("species")
    protected List<URI> species = new ArrayList<>();

    @JsonProperty("is_public")
    protected Boolean isPublic;

    @JsonProperty("facilities")
    protected List<URI> facilities = new ArrayList<>();

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<URI> getSpecies() {
        return species;
    }

    public void setSpecies(List<URI> species) {
        this.species = species;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<URI> facilities) {
        this.facilities = facilities;
    }

    protected static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {

        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream().map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ExperimentGetListDTO fromModel(ExperimentModel model) {

        ExperimentGetListDTO dto = new ExperimentGetListDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setStartDate(model.getStartDate());
        dto.setEndDate(model.getEndDate());
        dto.setIsPublic(model.getIsPublic());
        dto.setObjective(model.getObjective());
        dto.setDescription(model.getDescription());
        dto.setSpecies(getUriList(model.getSpecies()));
        dto.setFacilities(getUriList(model.getFacilities()));

        return dto;
    }
}
