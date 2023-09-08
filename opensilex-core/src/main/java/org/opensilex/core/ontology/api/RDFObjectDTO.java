/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 *
 * @author vmigot
 */
public class RDFObjectDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

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

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
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
        if (Objects.nonNull(publisher) && Objects.nonNull(publisher.getUri())) {
            model.setPublisher(publisher.getUri());
        }
        if (Objects.nonNull(publicationDate)) {
            model.setPublicationDate(publicationDate);
        }
//        if (!CollectionUtils.isEmpty(relations)) {
//            model.setRelations(relations.stream()
//                    .map(RDFObjectRelationDTO::toModel)
//                    .collect(Collectors.toList())
//            );
//        }
    }

}
