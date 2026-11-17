//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.List;

/**
 * A property the class of a sheet accepts, to choose the property of a column from.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyOptionDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("label")
    private String label;

    @JsonProperty("kind")
    @ApiModelProperty(value = "DATA for a literal, OBJECT for a link", example = "DATA")
    private String kind;

    @JsonProperty("range")
    private URI range;

    @JsonProperty("range_label")
    @ApiModelProperty(value = "The datatype, or the label of the class it links to", example = "date")
    private String rangeLabel;

    @JsonProperty("min_values")
    @ApiModelProperty(value = "How many values a row needs at least", example = "0")
    private int minValues;

    @JsonProperty("max_values")
    @ApiModelProperty(value = "How many values a row may hold at most; absent when unbounded", example = "1")
    private Integer maxValues;

    @JsonProperty("fitting_sheets")
    @ApiModelProperty(value = "For a link, the sheets whose rows its range accepts, in the workbook's order; absent "
            + "for a literal")
    private List<String> fittingSheets;

    /**
     * @param fittingSheets for a link, the sheets whose rows its range accepts; null for a literal
     */
    public static PropertyOptionDTO fromModel(PropertyView property, ClassCatalog catalog, String language,
                                              List<String> fittingSheets) {
        PropertyOptionDTO dto = new PropertyOptionDTO();
        dto.uri = property.uri();
        String label = property.labels().in(language);
        dto.label = label != null ? label : ClassCatalog.localName(property.uri());
        dto.kind = property.kind().name();
        dto.range = property.range();
        if (property.range() != null) {
            dto.rangeLabel = property.kind() == PropertyKind.OBJECT
                    ? catalog.labelOf(property.range(), language)
                    : ClassCatalog.localName(property.range());
        }
        dto.minValues = property.cardinality().min();
        dto.maxValues = property.cardinality().max();
        dto.fittingSheets = property.kind() == PropertyKind.OBJECT && fittingSheets != null
                ? List.copyOf(fittingSheets)
                : null;
        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public String getLabel() {
        return label;
    }

    public String getKind() {
        return kind;
    }

    public URI getRange() {
        return range;
    }

    public String getRangeLabel() {
        return rangeLabel;
    }

    public int getMinValues() {
        return minValues;
    }

    public List<String> getFittingSheets() {
        return fittingSheets;
    }

    public Integer getMaxValues() {
        return maxValues;
    }
}
