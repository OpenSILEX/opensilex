package org.opensilex.core.organisation.dal;

import org.apache.jena.vocabulary.ORG;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SPARQLResource(
        ontology = ORG.class,
        resource = "Site",
        graph = "set/infrastructures",
        prefix = "infra"
)
public class SiteModel extends SPARQLNamedResourceModel<SiteModel> {
    @SPARQLProperty(
            ontology = ORG.class,
            property = "siteAddress"
    )
    protected SiteAddressModel address;
    public static final String ADDRESS_FIELD = "address";

    @SPARQLProperty(
            ontology = ORG.class,
            property = "hasSite",
            ignoreUpdateIfNull = true,
            inverse = true
    )
    protected List<InfrastructureModel> organizations;
    public static final String ORGANIZATION_FIELD = "organizations";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            ignoreUpdateIfNull = true,
            property = "hasGroup"
    )
    protected List<GroupModel> groups;
    public static final String GROUP_FIELD = "groups";

    //@todo facilities


    public SiteAddressModel getAddress() {
        return address;
    }

    public void setAddress(SiteAddressModel address) {
        this.address = address;
    }

    public List<InfrastructureModel> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<InfrastructureModel> organizations) {
        this.organizations = organizations;
    }

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> groups) {
        this.groups = groups;
    }

    public List<URI> getOrganizationURIListOrEmpty() {
        if (getOrganizations() == null) {
            return new ArrayList<>();
        }

        return getOrganizations().stream()
                .map(InfrastructureModel::getUri)
                .collect(Collectors.toList());
    }
}
