/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * DTO representing JSON for update organisation
 * @author vince
 */
@ApiModel
@JsonPropertyOrder({"uri", "rdf_type", "name", "parent", "children", "groups"})
public class InfrastructureUpdateDTO extends InfrastructureCreationDTO {
    // Required for the update
    @Override
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}
