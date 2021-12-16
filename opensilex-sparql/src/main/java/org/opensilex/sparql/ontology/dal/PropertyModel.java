/*******************************************************************************
 *                         PropertyModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.ontology.dal;

import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.VocabularyModel;

import java.net.URI;

/**
 *
 * @author vince
 */
public interface PropertyModel<T extends VocabularyModel<T>> extends VocabularyModel<T> {

    ClassModel getDomain();

    void setDomain(ClassModel domain);

    URI getTypeRestriction();

    void setTypeRestriction(URI typeRestriction);

    default void fromModel(PropertyModel copy) {

        setUri(copy.getUri());
        setName(copy.getName());

        setType(copy.getType());
        if (copy.getTypeLabel() != null) {
            setTypeLabel(new SPARQLLabel(copy.getTypeLabel()));
        }

        if (copy.getLabel() != null) {
            setLabel(new SPARQLLabel(copy.getLabel()));
        }
        if (copy.getComment() != null) {
            setComment(new SPARQLLabel(copy.getComment()));
        }

        setTypeRestriction(copy.getTypeRestriction());
        if(copy.getDomain() != null){
            setDomain(new ClassModel(copy.getDomain(),false));
        }
    }

}
