//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.model;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.uri.generation.ClassURIGenerator;

/**
 *
 * @author vidalmor
 */
public class SPARQLNamedResourceModel<T extends SPARQLNamedResourceModel> extends SPARQLResourceModel implements ClassURIGenerator<T> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            ignoreUpdateIfNull = true
    )
    protected String name;
    public static final String NAME_FIELD = "name";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String[] getInstancePathSegments(T instance) {
        return new String[]{
            instance.getName()
        };
    }
    
}
