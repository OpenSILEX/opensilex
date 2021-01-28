//******************************************************************************
//                          AnnotationModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.annotation.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author Renaud COLIN
 */
@SPARQLResource(
        ontology = OA.class,
        resource = "Annotation",
        graph = "annotations"
)
public class AnnotationModel extends SPARQLResourceModel implements ClassURIGenerator<AnnotationModel> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "created",
            required = true
    )
    private OffsetDateTime created;

    @SPARQLProperty(
            ontology = OA.class,
            property = "bodyValue",
            required = true
    )
    private String bodyValue;

    public static final String BODY_VALUE_FIELD = "bodyValue";

    @SPARQLProperty(
            ontology = OA.class,
            property = "motivatedBy",
            required = true
    )
    private MotivationModel motivation;
    public static final String MOTIVATION_FIELD = "motivation";

    @SPARQLProperty(
            ontology = OA.class,
            property = "hasTarget",
            required = true
    )
    private List<URI> targets;
    public static final String TARGET_FIELD = "targets";

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public String getBodyValue() {
        return bodyValue;
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
    }

    public MotivationModel getMotivation() {
        return motivation;
    }

    public void setMotivation(MotivationModel motivation) {
        this.motivation = motivation;
    }

    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }

    @Override
    public String[] getUriSegments(AnnotationModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }

}
