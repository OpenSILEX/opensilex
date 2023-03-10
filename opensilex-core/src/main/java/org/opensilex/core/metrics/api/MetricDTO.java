//******************************************************************************
//                          MetricDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensilex.core.data.api.DataAPI;
import org.opensilex.core.metrics.dal.ExperimentSummaryModel;
import org.opensilex.core.metrics.dal.SystemSummaryModel;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author Arnaud Charleroy
 */
@ApiModel
@JsonPropertyOrder({"uri", "object_uri", "created_date", "items", "data_by_variables", "facilities", "factors"})
public class MetricDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("object_uri")
    protected URI objectURI;

    @ApiModelProperty(value = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, required = true)
    @JsonProperty("created_date")
    protected String createdDate;

    public void setDate(Instant instant, String offset, Boolean isDateTime) {

        if (isDateTime) {
            OffsetDateTime odt = instant.atOffset(offset == null ? ZoneOffset.UTC : ZoneOffset.of(offset));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSX.toString());
            this.setCreatedDate(dtf.format(odt));
        } else {
            LocalDate date = ZonedDateTime.ofInstant(instant, offset == null ? ZoneId.of(ZoneOffset.UTC.toString()) : ZoneId.of(offset)).toLocalDate();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMD.toString());
            this.setCreatedDate(dtf.format(date));
        }
    }

    @JsonProperty("items")
    private List<CountListItemDTO> items = new ArrayList<>();

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public URI getObjectURI() {
        return objectURI;
    }

    public void setObjectURI(URI objectURI) {
        this.objectURI = objectURI;
    }

    public List<CountListItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CountListItemDTO> items) {
        this.items = items;
    }

    public void addItems(CountListItemDTO item) {
        this.items.add(item);
    }

    public static MetricDTO getDTOfromExperimentSummaryModel(ExperimentSummaryModel model) {
        MetricDTO experimentSummaryDTO = new MetricDTO();
        
        CountListItemDTO variablesDTO = CountListItemDTO.getDTOFromModel(model.getDataByVariables());
        variablesDTO.setName(ExperimentSummaryModel.DATA_BY_VARIABLES);
        
        CountListItemDTO scientificObjectsByTypeDTO = CountListItemDTO.getDTOFromModel(model.getScientificObjectsByType());
        scientificObjectsByTypeDTO.setName(ExperimentSummaryModel.SCIENTIFIC_OBJECT_BY_TYPE_FIELD);
        
        experimentSummaryDTO.addItems(variablesDTO);
        experimentSummaryDTO.addItems(scientificObjectsByTypeDTO);
        
        experimentSummaryDTO.setDate(model.getCreationDate(), null, true);
        
        experimentSummaryDTO.setObjectURI(model.getExperimentUri());
        
        experimentSummaryDTO.setUri(model.getUri());

        return experimentSummaryDTO;
    }

    public static MetricDTO getDTOfromSystemSummaryModel(SystemSummaryModel model)  {
        MetricDTO systemSummaryDTO = new MetricDTO();
        CountListItemDTO variablesDTO = CountListItemDTO.getDTOFromModel(model.getDataByVariables());
        variablesDTO.setName(SystemSummaryModel.DATA_BY_VARIABLES);
        
        CountListItemDTO scientificObjectsByTypeDTO = CountListItemDTO.getDTOFromModel(model.getScientificObjectsByType());
        scientificObjectsByTypeDTO.setName(SystemSummaryModel.SCIENTIFIC_OBJECT_BY_TYPE_FIELD);
        
        CountListItemDTO devicesByTypeDTO = CountListItemDTO.getDTOFromModel(model.getDeviceByType());
        devicesByTypeDTO.setName(SystemSummaryModel.DEVICE_BY_TYPE_FIELD);
        
        CountListItemDTO germplasmByTypeDTO = CountListItemDTO.getDTOFromModel(model.getGermplasmByType());
        germplasmByTypeDTO.setName(SystemSummaryModel.GERMPLASM_TYPE_FIELD);

        CountListItemDTO experimentByTypeDTO = CountListItemDTO.getDTOFromModel(model.getExperimentByType());
        experimentByTypeDTO.setName(SystemSummaryModel.EXPERIMENT_BY_TYPE_FIELD);

        systemSummaryDTO.addItems(variablesDTO);
        systemSummaryDTO.addItems(scientificObjectsByTypeDTO);
        systemSummaryDTO.addItems(devicesByTypeDTO);
        systemSummaryDTO.addItems(germplasmByTypeDTO);
        systemSummaryDTO.addItems(experimentByTypeDTO);
        systemSummaryDTO.setDate(model.getCreationDate(), null, true);

        try {
            systemSummaryDTO.setObjectURI(new URI(model.getBaseSystemAlias() + ":system"));
        } catch (URISyntaxException ex) {
            Logger.getLogger(MetricDTO.class.getName()).log(Level.SEVERE, null, ex);
        }
        systemSummaryDTO.setUri(model.getUri());

        return systemSummaryDTO;
    }

}
