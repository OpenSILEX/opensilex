//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.mapping;

import java.net.*;
import org.opensilex.sparql.*;

/**
 *
 * @author vincent
 */
class SPARQLProxyResource<T> extends SPARQLProxy<T> {
    
    
    SPARQLProxyResource(URI uri, Class<T> type, SPARQLService service) {
        super(type, service);
        this.uri = uri;
    }
    
    protected final URI uri;

    @Override
    protected T loadData() throws Exception {
        return service.loadByURI(type, uri);
    }

}
