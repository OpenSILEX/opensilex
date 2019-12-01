//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.user.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLModel;
import org.opensilex.sparql.utils.ClassURIGenerator;


/**
 *
 * @author Vincent Migot
 */
@SPARQLResource(
        ontology = UserOntology.class,
        resource = "Group"
)
public class Group extends SPARQLModel implements ClassURIGenerator<Group> {

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title"
    )
    private String name;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String[] getUriSegments(Group instance) {
        return new String[]{
            "groups",
            instance.getName()
        };
    }

}
