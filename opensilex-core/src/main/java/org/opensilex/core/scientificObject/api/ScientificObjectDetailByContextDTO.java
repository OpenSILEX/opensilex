/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.scientificObject.dal.ExperimentalObjectModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;

import java.net.URI;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDetailByContextDTO extends ScientificObjectDetailDTO {

    private URI context;

    private String contextLabel;

    private URI contextType;

    private String contextTypeLabel;

    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public void setContextLabel(String contextLabel) {
        this.contextLabel = contextLabel;
    }

    public URI getContextType() {
        return contextType;
    }

    public void setContextType(URI contextType) {
        this.contextType = contextType;
    }

    public String getContextTypeLabel() {
        return contextTypeLabel;
    }

    public void setContextTypeLabel(String contextTypeLabel) {
        this.contextTypeLabel = contextTypeLabel;
    }

    @Override
    public void toModel(ExperimentalObjectModel model) {
        super.toModel(model);
    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    static ScientificObjectDetailByContextDTO getDTOFromModel(ExperimentalObjectModel model, ScientificObjectContextModel context, GeospatialModel geometryByURI) throws JsonProcessingException {
        ScientificObjectDetailByContextDTO dto = new ScientificObjectDetailByContextDTO();
        dto.fromModel(model);
        if (geometryByURI != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        dto.setContext(context.getContext());
        dto.setContextLabel(context.getContextLabel());
        dto.setContextType(context.getContextType());
        dto.setContextTypeLabel(context.getContextTypeLabel());

        return dto;
    }
}
