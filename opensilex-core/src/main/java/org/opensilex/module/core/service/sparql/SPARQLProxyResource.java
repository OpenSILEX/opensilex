/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql;

import java.net.URI;

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
        return service.getByURI(type, uri);
    }

}
