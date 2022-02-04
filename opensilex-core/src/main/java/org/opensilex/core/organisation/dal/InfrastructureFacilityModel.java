/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.organisation.dal;

import org.apache.jena.vocabulary.VCARD4;
import org.opensilex.core.ontology.Oeso;
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
        graph = InfrastructureModel.GRAPH,
        prefix = "infra"
)
public class InfrastructureFacilityModel extends SPARQLTreeModel<InfrastructureFacilityModel> {

    private static final String FACILITY = "facility";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf"
    )
    protected InfrastructureFacilityModel parent;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isPartOf",
            inverse = true,
            ignoreUpdateIfNull = true,
            useDefaultGraph = false
    )
    protected List<InfrastructureFacilityModel> children;

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "isHosted",
            inverse = true
    )
    private List<InfrastructureModel> infrastructures;
    public static final String INFRASTRUCTURE_FIELD = "infrastructures";

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

    public List<InfrastructureModel> getInfrastructures() {
        return infrastructures;
    }

    public void setInfrastructures(List<InfrastructureModel> infrastructures) {
        this.infrastructures = infrastructures;
    }

    public List<URI> getInfrastructureUris() {
        if (this.infrastructures == null) {
            return new ArrayList<>();
        }
        return this.infrastructures
                .stream()
                .map(InfrastructureModel::getUri)
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

    @Override
    public String[] getInstancePathSegments(SPARQLTreeModel<InfrastructureFacilityModel> instance) {
        return new String[]{
                FACILITY,
                instance.getName()
        };
    }

}
