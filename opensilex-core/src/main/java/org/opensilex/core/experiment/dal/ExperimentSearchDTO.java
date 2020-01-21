package org.opensilex.core.experiment.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.core.experiment.api.ExperimentGetDTO;


/**
 * @author Renaud COLIN
 */
public class ExperimentSearchDTO extends ExperimentGetDTO {

    protected final static ObjectMapper objectMapper = new ObjectMapper();

    public static ExperimentSearchDTO valueOf(String value) throws JsonProcessingException {
        ExperimentSearchDTO dto = objectMapper.readValue(value,ExperimentSearchDTO.class);
        return dto;
    }

    private Boolean ended;

    public Boolean isEnded() {
        return ended;
    }

    public ExperimentSearchDTO setEnded(Boolean ended) {
        this.ended = ended;
        return this;
    }
}
