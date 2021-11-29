/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal;

import java.util.List;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLDagModel;

/**
 *
 * @author vince
 */
@SPARQLResource(
        ontology = FOAF.class,
        resource = "Organization",
        graph = InfrastructureModel.GRAPH,
        prefix = "infra"
)
public class InfrastructureModel extends SPARQLDagModel<InfrastructureModel> {

    public static final String GRAPH = "organization";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<InfrastructureModel> parents;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            ignoreUpdateIfNull = true
    )
    protected List<InfrastructureModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            ignoreUpdateIfNull = true
    )
    private List<InfrastructureFacilityModel> facilities;
    public static final String FACILITIES_FIELD = "facilities";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            property = "hasGroup",
            cascadeDelete = true,
            ignoreUpdateIfNull = true
    )
    private List<GroupModel> groups;
    public static final String GROUP_FIELD = "groups";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "usesOrganization",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    private List<ExperimentModel> experiments;
    public static final String EXPERIMENT_FIELD = "experiments";

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> group) {
        this.groups = group;
    }

    public List<InfrastructureFacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<InfrastructureFacilityModel> facilities) {
        this.facilities = facilities;
    }

    public List<ExperimentModel> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentModel> experiments) {
        this.experiments = experiments;
    }
}
