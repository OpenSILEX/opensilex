/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import java.util.Map;
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.PropertyModel;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author vmigot
 */
public class RDFPropertyDTO {

    public final static String DATA_PROPERTY = "dataProperty";

    public final static String OBJECT_PROPERTY = "objectProperty";

    protected URI uri;

    protected String propertyType;

    protected Map<String, String> label;

    protected Map<String, String> comment;

    protected URI domain;

    protected URI range;

    protected URI parent;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(Map<String, String> comment) {
        this.comment = comment;
    }

    public URI getDomain() {
        return domain;
    }

    public void setDomain(URI domain) {
        this.domain = domain;
    }

    public URI getRange() {
        return range;
    }

    public void setRange(URI range) {
        this.range = range;
    }

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public PropertyModel toModel() {
        PropertyModel model;

        boolean isDataProperty = getPropertyType().equals(DATA_PROPERTY);

        if (isDataProperty) {
            DatatypePropertyModel dtModel = new DatatypePropertyModel();
            if (getParent() != null) {
                DatatypePropertyModel parentModel = new DatatypePropertyModel();
                parentModel.setUri(getParent());
                dtModel.setParent(parentModel);
            }
            model = dtModel;
        } else {
            ObjectPropertyModel objModel = new ObjectPropertyModel();
            if (getParent() != null) {
                ObjectPropertyModel parentModel = new ObjectPropertyModel();
                parentModel.setUri(getParent());
                objModel.setParent(parentModel);
            }
            model = objModel;
        }

        model.setLabel(SPARQLLabel.fromMap(getLabel()));
        model.setComment(SPARQLLabel.fromMap(getComment()));
        model.setDomain(getDomain());
        model.setRange(getRange());

        return model;
    }

    public static RDFPropertyDTO fromModel(PropertyModel model, PropertyModel parentModel) {
        RDFPropertyDTO dto = new RDFPropertyDTO();

        dto.setUri(model.getUri());
        if (parentModel != null) {
            dto.setParent(parentModel.getUri());
        }
        dto.setLabel(model.getLabel().getAllTranslations());
        dto.setComment(model.getComment().getAllTranslations());
        dto.setDomain(model.getDomain());
        dto.setRange(model.getRange());

        return dto;
    }

}
