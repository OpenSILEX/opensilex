//******************************************************************************
//                          EventModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

import java.net.URI;
import java.util.List;
import java.util.UUID;



/**
 * @author Renaud COLIN
 */
@SPARQLResource(
        ontology = Oeev.class,
        resource = "Event",
        graph = EventModel.GRAPH
)
public class EventModel extends SPARQLResourceModel implements ClassURIGenerator<EventModel> {

    public static final String GRAPH = "set/events";

    @SPARQLProperty(
            ontology = Oeev.class,
            property = "concerns",
            required = true
    )
    private List<URI> targets;
    public static final String TARGETS_FIELD = "targets";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    @SPARQLProperty(
            ontology = Time.class,
            property = "hasBeginning"
    )
    private InstantModel start;
    public static final String START_FIELD = "start";

    @SPARQLProperty(
            ontology = Time.class,
            property = "hasEnd"
    )
    private InstantModel end;
    public static final String END_FIELD = "end";

    @SPARQLProperty(
            ontology = Oeev.class,
            property = "isInstant",
            required = true
    )
    private Boolean isInstant;
    public static final String IS_INSTANT_FIELD = "isInstant";


    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }

    public InstantModel getStart() {
        return start;
    }

    public void setStart(InstantModel start) {
        this.start = start;
    }

    public InstantModel getEnd() {
        return end;
    }

    public void setEnd(InstantModel end) {
        this.end = end;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsInstant() {
        return isInstant;
    }

    public void setIsInstant(Boolean interval) {
        isInstant = interval;
    }

    @Override
    public String[] getUriSegments(EventModel instance) {
        return new String[]{
                UUID.randomUUID().toString()
        };
    }
}
