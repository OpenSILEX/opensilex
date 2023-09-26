/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author vmigot
 */
public class RDFObjectDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("relations")
    protected List<RDFObjectRelationDTO> relations;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public List<RDFObjectRelationDTO> getRelations() {
        return relations;
    }

    public void setRelations(List<RDFObjectRelationDTO> relations) {
        this.relations = relations;
    }

    public void toModel(SPARQLResourceModel model){
        model.setUri(uri);
        model.setType(type);

//        if (!CollectionUtils.isEmpty(relations)) {
//            model.setRelations(relations.stream()
//                    .map(RDFObjectRelationDTO::toModel)
//                    .collect(Collectors.toList())
//            );
//        }
    }

}
