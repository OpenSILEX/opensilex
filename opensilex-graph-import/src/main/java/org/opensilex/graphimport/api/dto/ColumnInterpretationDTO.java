//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.ColumnInterpretation;
import org.opensilex.graphimport.interpret.MeasureChoice;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;

/**
 * What a column holds.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnInterpretationDTO {

    @JsonProperty("index")
    @ApiModelProperty(value = "Position of the column, from 0", example = "0")
    private int index;

    @JsonProperty("header")
    private String header;

    @JsonProperty("header_form")
    @ApiModelProperty(value = "How the header is written: FULL_URI, PREFIXED_NAME, TYPE, UNKNOWN_PREFIX or PLAIN",
            example = "PLAIN")
    private String headerForm;

    @JsonProperty("language")
    @ApiModelProperty(value = "The language of a @fr suffix of the header", example = "fr")
    private String language;

    @JsonProperty("identifier")
    @ApiModelProperty(value = "True when the column identifies the rows of its sheet")
    private boolean identifier;

    @JsonProperty("type_column")
    @ApiModelProperty(value = "True when the column gives the class of each row")
    private boolean typeColumn;

    @JsonProperty("property")
    private ChoiceDTO property;

    @JsonProperty("given")
    @ApiModelProperty(value = "True when the column holds a value the user gave for every row")
    private boolean given;

    @JsonProperty("variable")
    @ApiModelProperty(value = "For a column of measures in a sheet of data, the variable it measures")
    private URI variable;

    @JsonProperty("variable_status")
    @ApiModelProperty(value = "CHOSEN or SUGGESTED")
    private String variableStatus;

    @JsonProperty("variable_label")
    private String variableLabel;

    static ColumnInterpretationDTO fromModel(ColumnInterpretation column, ClassView classView, String language) {
        ColumnInterpretationDTO dto = new ColumnInterpretationDTO();
        dto.index = column.index();
        dto.header = column.header();
        dto.headerForm = column.reading().form().name();
        dto.language = column.reading().language();
        dto.identifier = column.identifier();
        dto.typeColumn = column.typeColumn();
        dto.property = ChoiceDTO.ofUri(column.property(), column.property().candidates().stream()
                .map(candidate -> {
                    PropertyView property = classView == null ? null : classView.getProperty(candidate.target());
                    String label = property == null ? null : property.labels().in(language);
                    return CandidateDTO.ofProperty(candidate,
                            label != null ? label : ClassCatalog.localName(candidate.target()), property);
                })
                .toList());
        return dto;
    }

    public int getIndex() {
        return index;
    }

    public String getHeader() {
        return header;
    }

    public String getHeaderForm() {
        return headerForm;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isIdentifier() {
        return identifier;
    }

    public boolean isTypeColumn() {
        return typeColumn;
    }

    public ChoiceDTO getProperty() {
        return property;
    }

    public boolean isGiven() {
        return given;
    }

    public void setGiven(boolean given) {
        this.given = given;
    }

    /** Says the variable the column measures. */
    public void setMeasure(MeasureChoice measure) {
        variable = measure.variable();
        variableStatus = measure.status().name();
        variableLabel = measure.label();
    }

    public URI getVariable() {
        return variable;
    }

    public String getVariableStatus() {
        return variableStatus;
    }

    public String getVariableLabel() {
        return variableLabel;
    }
}
