//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr

package org.opensilex.core.ontology;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.SKOSReferencesModel;
import org.opensilex.sparql.annotations.SPARQLProperty;

/**
 * @author renaud.colin@inra.fr
 */
public abstract class SKOSReferencesLabeledModel extends SKOSReferencesModel {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    private String label;
    public static final String LABEL_FIELD = "label";

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    private String comment;
    public static final String COMMENT_FIELD = "comment";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
