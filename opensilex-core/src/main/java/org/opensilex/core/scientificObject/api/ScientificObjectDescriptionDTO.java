/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import org.opensilex.core.ontology.api.RDFObjectDTO;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDescriptionDTO extends RDFObjectDTO {

    private URI experiment;

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public static ScientificObjectCsvDescriptionDTO fromString(String param) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(param, ScientificObjectCsvDescriptionDTO.class);
    }

}
