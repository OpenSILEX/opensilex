/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal;

import java.util.List;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
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
        graph = OrganizationModel.GRAPH,
        prefix = "infra"
)
public class OrganizationModel extends SPARQLDagModel<OrganizationModel> {

    public static final String GRAPH = "organization";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected List<OrganizationModel> parents;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasPart",
            ignoreUpdateIfNull = true
    )
    protected List<OrganizationModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            ignoreUpdateIfNull = true
    )
    private List<FacilityModel> facilities;
    public static final String FACILITIES_FIELD = "facilities";

    @SPARQLProperty(
            ontology = ORG.class,
            property = "hasSite",
            ignoreUpdateIfNull = true
    )
    private List<SiteModel> sites;
    public static final String SITE_FIELD = "site";

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

    @Override
    public List<OrganizationModel> getParents() {
        return parents;
    }

    @Override
    public void setParents(List<OrganizationModel> parents) {
        this.parents = parents;
    }

    @Override
    public List<OrganizationModel> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<OrganizationModel> children) {
        this.children = children;
    }

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> group) {
        this.groups = group;
    }

    public List<FacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<FacilityModel> facilities) {
        this.facilities = facilities;
    }

    public List<SiteModel> getSites() {
        return sites;
    }

    public void setSites(List<SiteModel> sites) {
        this.sites = sites;
    }

    public List<ExperimentModel> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<ExperimentModel> experiments) {
        this.experiments = experiments;
    }
}
