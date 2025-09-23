//******************************************************************************
//                          ProvEntityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.opensilex.core.provenance.dal.GlobalProvenanceEntity;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.server.rest.serialization.uri.UriJsonDeserializer;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Objects;

/**
 * Entity model used in "prov:used"
 * @author Alice Boizet
 */
public class ProvEntityModel {

    @ValidURI
    @JsonProperty("rdf_type")
    @JsonDeserialize(using = UriJsonDeserializer.class)
    URI type;

    @ValidURI
    @NotNull
    @JsonDeserialize(using = UriJsonDeserializer.class)
    URI uri;

    public ProvEntityModel(){}

    public ProvEntityModel(URI uri, URI type) {
        setUri(uri);
        setType(type);
    }

    public ProvEntityModel(SPARQLResourceModel model) {
        this(model.getUri(), model.getType());
    }

    public ProvEntityModel(MongoModel model) {
        this(model.getUri(), model.getRdfType());
    }

    public ProvEntityModel(GlobalProvenanceEntity entity){
        this(entity.getUri(), entity.getRdfType());
    }

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
