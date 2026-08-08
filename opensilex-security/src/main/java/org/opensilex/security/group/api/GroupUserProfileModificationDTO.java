/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vidalmor
 */
@Schema
public class GroupUserProfileModificationDTO extends GroupUserProfileDTO {

    @ValidURI
    @NotNull
    @Schema(description = "User associated profile URI", requiredMode = Schema.RequiredMode.REQUIRED)
    public URI getProfileURI() {
        return this.profileURI;
    }

    @ValidURI
    @NotNull
    @Schema(description = "User URI", requiredMode = Schema.RequiredMode.REQUIRED)
    public URI getUserURI() {
        return this.userURI;
    }
}
