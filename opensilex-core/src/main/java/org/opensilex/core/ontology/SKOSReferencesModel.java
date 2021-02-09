//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, morgane.vidal@inra.fr,anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.ontology;

import java.net.URI;
import java.util.List;
import org.apache.jena.vocabulary.SKOS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * Ontology reference model.
 *
 * @author Morgane Vidal
 */
public abstract class SKOSReferencesModel extends SPARQLResourceModel implements SKOSReferences {

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "exactMatch"
    )
    private List<URI> exactMatch;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "closeMatch"
    )
    private List<URI> closeMatch;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "broadMatch"
    )
    private List<URI> broadMatch;

    @SPARQLProperty(
            ontology = SKOS.class,
            property = "narrowMatch"
    )
    private List<URI> narrowMatch;

    public List<URI> getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(List<URI> exactMatch) {
        this.exactMatch = exactMatch;
    }

    public List<URI> getCloseMatch() {
        return closeMatch;
    }

    public void setCloseMatch(List<URI> closeMatch) {
        this.closeMatch = closeMatch;
    }

    public List<URI> getBroadMatch() {
        return broadMatch;
    }

    public void setBroadMatch(List<URI> broadMatch) {
        this.broadMatch = broadMatch;
    }

    public List<URI> getNarrowMatch() {
        return narrowMatch;
    }

    public void setNarrowMatch(List<URI> narrowMatch) {
        this.narrowMatch = narrowMatch;
    }

}

