//******************************************************************************
//                          ProvEntityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Objects;

/**
 * Entity model used in "prov:used"
 * @author Alice Boizet
 */
public class ProvEntityModel {

    @ValidURI
    @NotNull
    @JsonProperty("rdf_type")
    URI type;

    @ValidURI
    @NotNull
    URI uri;

    public static final String URI_FIELD = "uri";
    public static final String TYPE_FIELD = "rdfType";

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProvEntityModel that = (ProvEntityModel) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}
