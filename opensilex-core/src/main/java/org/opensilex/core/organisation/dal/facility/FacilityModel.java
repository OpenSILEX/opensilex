/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal.facility;

import org.apache.jena.vocabulary.VCARD4;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.core.variablesGroup.dal.VariablesGroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLTreeModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Facility",
        graph = OrganizationModel.GRAPH,
        prefix = "infra"
)
public class FacilityModel extends SPARQLTreeModel<FacilityModel> {

    private static final String FACILITY = "facility";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected FacilityModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true,
            useDefaultGraph = false
    )
    protected List<FacilityModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            inverse = true
    )
    private List<OrganizationModel> organizations;
    public static final String ORGANIZATIONS_FIElD = "organizations";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "withinSite",
            ignoreUpdateIfNull = true
    )
    private List<SiteModel> sites;
    public static final String SITE_FIELD = "sites";

    @SPARQLProperty(
            ontology = VCARD4.class,
            property = "hasAddress",
            cascadeDelete = true
    )
    private FacilityAddressModel address;
    public static final String ADDRESS_FIELD = "address";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "hasVariablesGroup",
            ignoreUpdateIfNull = true
    )
    private List<VariablesGroupModel> variableGroups;
    public static final String VARIABLE_GROUPS_FIELD = "variableGroups";

    public List<OrganizationModel> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<OrganizationModel> organizations) {
        this.organizations = organizations;
    }

    public List<URI> getOrganizationUriList() {
        if (this.organizations == null) {
            return new ArrayList<>();
        }
        return this.organizations
                .stream()
                .map(OrganizationModel::getUri)
                .collect(Collectors.toList());
    }

    public List<SiteModel> getSites() {
        return sites;
    }

    public void setSites(List<SiteModel> sites) {
        this.sites = sites;
    }

    public FacilityAddressModel getAddress() {
        return address;
    }

    public void setAddress(FacilityAddressModel address) {
        this.address = address;
    }

    public List<VariablesGroupModel> getVariableGroups() {
        return variableGroups;
    }

    public void setVariableGroups(List<VariablesGroupModel> variableGroups) {
        this.variableGroups = variableGroups;
    }

    @Override
    public String[] getInstancePathSegments(SPARQLTreeModel<FacilityModel> instance) {
        return new String[]{
                FACILITY,
                instance.getName()
        };
    }

}
