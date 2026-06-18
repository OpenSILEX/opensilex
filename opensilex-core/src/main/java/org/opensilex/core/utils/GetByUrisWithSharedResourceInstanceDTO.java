/*
 * *****************************************************************************
 *                         GetByUrisWithSharedResourceInstanceDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2026.
 * Last Modification: 18/06/2026 15:07
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.utils;

import java.net.URI;
import java.util.List;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * DTO for POST endpoints that accept a list of URIs along with an optional shared resource instance parameter.
 * Used by APIs that support both local and shared resource instances (e.g., Variables, Characteristics, Entities, etc.).
 */
public class GetByUrisWithSharedResourceInstanceDTO {
    
    @ValidURI
    protected List<URI> uris;
    
    @ValidURI
    protected URI sharedResourceInstance;

    public List<URI> getUris() {
        return uris;
    }

    public void setUris(List<URI> uris) {
        this.uris = uris;
    }
    
    public URI getSharedResourceInstance() {
        return sharedResourceInstance;
    }
    
    public void setSharedResourceInstance(URI sharedResourceInstance) {
        this.sharedResourceInstance = sharedResourceInstance;
    }
    
}

