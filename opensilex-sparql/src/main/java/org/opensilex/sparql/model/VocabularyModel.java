/*******************************************************************************
 *                         VocabularyModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.model;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;

public abstract class VocabularyModel<T extends VocabularyModel<T>> extends SPARQLTreeModel<T>
        implements TranslatedModel, DagModel<T> {


    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label"
    )
    protected SPARQLLabel label;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    protected SPARQLLabel comment;

    @Override
    public SPARQLLabel getLabel() {
        return label;
    }

    @Override
    public void setLabel(SPARQLLabel label) {
        this.label = label;
    }

    @Override
    public SPARQLLabel getComment() {
        return comment;
    }

    @Override
    public void setComment(SPARQLLabel comment) {
        this.comment = comment;
    }

    @Override
    public String getName() {
        SPARQLLabel slabel = getLabel();
        if (slabel != null) {
            return getLabel().getDefaultValue();
        } else {
            return getUri().toString();
        }
    }
}
