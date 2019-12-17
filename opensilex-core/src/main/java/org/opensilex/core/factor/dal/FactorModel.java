/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.factor.dal;

import java.time.LocalDate;
import java.util.List;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.server.security.dal.GroupModel;
import org.opensilex.server.security.dal.UserModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.utils.ClassURIGenerator;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Factor",
        graph = "set/factors",
        prefix = "factor"
)
public class FactorModel extends SPARQLResourceModel implements ClassURIGenerator<FactorModel> {

    @SPARQLProperty(
            ontology = RDFS.class,
            property = "label",
            required = true
    )
    String alias;
    public static final String ALIAS_FIELD = "alias";

 
    @SPARQLProperty(
            ontology = RDFS.class,
            property = "comment"
    )
    String comment;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }


    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String[] getUriSegments(FactorModel instance) {
        return new String[]{
            instance.getAlias()
        };
    }
}
