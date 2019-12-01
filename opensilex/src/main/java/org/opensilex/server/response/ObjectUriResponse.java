//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import java.net.URI;
import javax.ws.rs.core.Response.Status;


/**
 *
 * @author vincent
 */
public class ObjectUriResponse extends JsonResponse<String> {
    
    public ObjectUriResponse(Status status, URI uri) {
        super(status);
        this.metadata = new Metadata(new Pagination());
        this.metadata.addDataFile(uri);
    }
    
}
