package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.sparql.ontology.dal.AbstractPropertyModel;
import org.opensilex.sparql.ontology.dal.ObjectPropertyModel;

/**
 * @author rcolin
 */
public class RDFPropertyGetDTO extends RDFPropertyDTO {

    @ApiModelProperty(
            value = "Default property name according language",
            example = "custom_object_property"
    )
    protected String name;

    @ApiModelProperty(
            value = "Default property description according language",
            example = "Description of the property"
    )
    protected String comment;

    @JsonProperty("range_label")
    protected String rangeLabel;

    public RDFPropertyGetDTO() {
    }

    public RDFPropertyGetDTO(AbstractPropertyModel<?> model, String lang) {
        super(model);

        setName(model.getLabel().getAllTranslations().get(lang));
        if (model.getComment() != null) {
            setComment(model.getComment().getAllTranslations().get(lang));
        }

        if (model instanceof ObjectPropertyModel && ((ObjectPropertyModel) model).getRange() != null) {
            ObjectPropertyModel objectProperty = (ObjectPropertyModel) model;
            setRangeLabel(objectProperty.getRange().getLabel().getAllTranslations().get(lang));
        }
    }

    public String getRangeLabel() {
        return rangeLabel;
    }

    public void setRangeLabel(String rangeLabel) {
        this.rangeLabel = rangeLabel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
