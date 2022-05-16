/*******************************************************************************
 *                         AbstractPropertyModel.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.model.VocabularyModel;

import java.net.URI;

public abstract class AbstractPropertyModel<T extends VocabularyModel<T>> extends VocabularyModel<T> implements PropertyModel {

    protected URI typeRestriction;

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "domain"
    )
    protected ClassModel domain;
    public static final  String DOMAIN_FIELD = "domain";

    public static final String RANGE_FIELD = "range";

    @Override
    public URI getTypeRestriction(){
        return typeRestriction;
    }

    @Override
    public void setTypeRestriction(URI typeRestriction) {
        this.typeRestriction = typeRestriction;
    }

    @Override
    public ClassModel getDomain() {
        return domain;
    }

    @Override
    public void setDomain(ClassModel domain) {
        this.domain = domain;
    }
}
