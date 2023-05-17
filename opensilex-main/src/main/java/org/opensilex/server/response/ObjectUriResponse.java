//******************************************************************************
//                          ObjectUriResponse.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.net.URI;
import java.util.List;
import javax.ws.rs.core.Response.Status;

/**
 * <pre>
 * Simple Object URI response.
 *
 * For a resource CREATED response, you might want to use `CreatedUriResponse` instead (from `opensilex-sparql`).
 *
 * Automatically define datafile metadata the result body with the given URI.
 * </pre>
 *
 * @see org.opensilex.server.response.JsonResponse
 * @author Vincent Migot
 */
public class ObjectUriResponse extends JsonResponse<String> {

    /**
     * Generic constructor.
     *
     * @param status response status
     * @param uri object uri
     */
    public ObjectUriResponse(Status status, URI uri) {
        super(status);
        this.metadata = new MetadataDTO(new PaginationDTO());
        this.metadata.addDataFile(uri);
        this.result = uri.toString();
    }

    /**
     * Generic constructor for list of URI.
     *
     * @param status response status
     * @param uris list uri
     */
    public ObjectUriResponse(Status status, List<URI> uris) {
        super(status);
        this.metadata = new MetadataDTO(new PaginationDTO());
        uris.forEach(uri -> {
            this.metadata.addDataFile(uri);
        });
        this.result = uris.toString();
    }
    
    /**
     * Constructor OK for an URI.
     *
     * @param uri object uri
     */
    public ObjectUriResponse(URI uri) {
        this(Status.OK, uri);
    }

    /**
     * Empty constructor with OK status and no URI.
     */
    public ObjectUriResponse() {
        super(Status.OK);
    }
}
