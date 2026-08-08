//******************************************************************************
//                        ApiExternalDocsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.system.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Class that represents informations about external docs
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"description", "url"})
public class ApiExternalDocsDTO {
    
    @Schema(description = "Opensilex api docs", example = "Opensilex api docs")
    private String description;
    
    @Schema(description = "https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/index.md",
                      example = "https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/index.md")
    private String url;

    public ApiExternalDocsDTO() {
    }

    public ApiExternalDocsDTO(String description, String url) {
        this.description = description;
        this.url = url;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
