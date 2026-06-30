//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.annotations.*;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.utils.Ontology;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author vidalmor
 */
@SPARQLResource(
        ontology = OWL2.class,
        resource = "Class",
        ignoreValidation = true
)
public class SPARQLResourceModel implements SPARQLModel {

    @SPARQLResourceURI()
    protected URI uri;
    public static final String URI_FIELD = "uri";

    @SPARQLTypeRDF()
    protected URI rdfType;
    public static final String TYPE_FIELD = "rdfType";

    @SPARQLTypeRDFLabel()
    protected SPARQLLabel rdfTypeName;
    public static final String TYPE_NAME_FIELD = "rdfTypeName";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "publisher"
    )
    protected URI publisher;
    public static final String PUBLISHER_FIELD = "publisher";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "issued"
    )
    protected OffsetDateTime publicationDate;
    public static final String PUBLICATION_DATE_FIELD = "publicationDate";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "modified"
    )
    protected OffsetDateTime lastUpdateDate;
    public static final String LAST_UPDATE_DATE_FIELD = "lastUpdateDate";

    protected List<SPARQLModelRelation> relations = new ArrayList<>();

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return rdfType;
    }

    public void setType(URI type) {
        this.rdfType = type;
    }

    public SPARQLLabel getTypeLabel() {
        return rdfTypeName;
    }

    public void setTypeLabel(SPARQLLabel typeLabel) {
        this.rdfTypeName = typeLabel;
    }

    public URI getPublisher() { return publisher; }

    public void setPublisher(URI publisher) { this.publisher = publisher; }

    public OffsetDateTime getPublicationDate() { return publicationDate; }

    public void setPublicationDate(OffsetDateTime publicationDate) { this.publicationDate = publicationDate; }

    public OffsetDateTime getLastUpdateDate() { return lastUpdateDate; }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) { this.lastUpdateDate = lastUpdateDate; }

    public List<SPARQLModelRelation> getRelations() {
        return relations;
    }
    
    public SPARQLModelRelation getRelation(Property relation) {
        Optional<SPARQLModelRelation> result = relations.stream().filter((r) -> {
            return SPARQLDeserializers.compareURIs(r.getProperty().getURI(), relation.getURI());
        }).findFirst();

        return result.orElse(null);
    }
     public Stream <SPARQLModelRelation> getRelations(Property relation) {
       return  relations.stream().filter((r) -> {
            return SPARQLDeserializers.compareURIs(r.getProperty().getURI(), relation.getURI());
        }); 
    }

    public void setRelations(List<SPARQLModelRelation> relations) {
        this.relations = relations;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.uri);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SPARQLResourceModel other = (SPARQLResourceModel) obj;
        if (this.uri == null || other.uri == null) {
            return false;
        }
        return SPARQLDeserializers.compareURIs(this.uri, other.uri);
    }

    public void addRelation(URI graph, URI propertyURI, Class<?> type, String value) {
        if (this.relations == null) {
            this.relations = new ArrayList<>();
        }

        SPARQLModelRelation r = new SPARQLModelRelation();
        r.setGraph(graph);
        r.setProperty(Ontology.property(propertyURI));
        r.setType(type);
        r.setValue(value);
        r.setReverse(false);

        this.relations.add(r);
    }

    public static List<URI> getUriList(List<? extends SPARQLResourceModel> models) {
        if (models == null || models.isEmpty()) {
            return Collections.emptyList();
        }
        return models.stream()
                .map(SPARQLResourceModel::getUri)
                .collect(Collectors.toCollection(ArrayList::new));
    }


}
