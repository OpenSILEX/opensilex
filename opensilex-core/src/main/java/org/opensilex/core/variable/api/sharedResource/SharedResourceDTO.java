package org.opensilex.core.variable.api.sharedResource;

import org.opensilex.sparql.response.ObjectNamedResourceDTO;

import java.net.URI;

public class SharedResourceDTO extends ObjectNamedResourceDTO {

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
