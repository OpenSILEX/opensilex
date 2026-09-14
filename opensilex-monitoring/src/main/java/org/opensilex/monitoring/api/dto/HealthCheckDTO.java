//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.Instant;
import java.util.List;

/**
 * Liveness of the instance and of every database it depends on. Carries no volumetry: those figures are admin only and live behind /monitoring/dbstats.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class HealthCheckDTO {

    @ApiModelProperty(value = "worst status among the components")
    @JsonProperty("status")
    private String status;

    @ApiModelProperty(value = "when the probes ran")
    @JsonProperty("checked_at")
    private Instant checkedAt;

    @ApiModelProperty(value = "one entry per probed component")
    @JsonProperty("components")
    private List<ComponentHealthDTO> components;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public List<ComponentHealthDTO> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentHealthDTO> components) {
        this.components = components;
    }

}
