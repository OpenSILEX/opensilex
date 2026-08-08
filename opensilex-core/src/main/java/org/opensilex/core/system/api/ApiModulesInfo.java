//******************************************************************************
//                        ApiModulesInfo.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.system.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Informations about loaded modules
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"name", "version"})
public class ApiModulesInfo {

    @Schema(description = "opensilex-core", example = "opensilex-core")
    private String name;

    @Schema(description = "1.0.0-beta+2", example = "1.0.0-beta+2")
    private String version;

    public ApiModulesInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}