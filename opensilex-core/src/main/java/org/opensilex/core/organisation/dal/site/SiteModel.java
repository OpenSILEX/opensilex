package org.opensilex.core.organisation.dal.site;

import org.apache.jena.vocabulary.ORG;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The physical location of an organization hat may have an address.
 *
 * @author Valentin RIGOLLE
 */
@SPARQLResource(
        ontology = ORG.class,
        resource = "Site",
        graph = OrganizationModel.GRAPH,
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
            ontology = RDFS.class,
            property = "comment"
    )
    String description;

    @SPARQLProperty(
            ontology = ORG.class,
            property = "hasSite",
            ignoreUpdateIfNull = true,
            inverse = true
    )
    protected List<OrganizationModel> organizations;
    public static final String ORGANIZATION_FIELD = "organizations";

    @SPARQLProperty(
            ontology = SecurityOntology.class,
            ignoreUpdateIfNull = true,
            property = "hasGroup"
    )
    protected List<GroupModel> groups;
    public static final String GROUP_FIELD = "groups";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "withinSite",
            ignoreUpdateIfNull = true,
            inverse = true
    )
    protected List<FacilityModel> facilities;
    public static final String FACILITY_FIELD = "facilities";

    @SPARQLProperty(
            ontology = SOSA.class,
            property = "hasFeatureOfInterest",
            inverse = true,
            ignoreUpdateIfNull = true
    )
    protected LocationObservationCollectionModel locationObservationCollection;
    public static final String OBSERVATION_COLLECTION_FIELD = "LocationObservationCollection";

    public SiteAddressModel getAddress() {
        return address;
    }

    public void setAddress(SiteAddressModel address) {
        this.address = address;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public List<OrganizationModel> getOrganizations() {
        return organizations;
    }

    public void setOrganizations(List<OrganizationModel> organizations) {
        this.organizations = organizations;
    }

    public List<GroupModel> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupModel> groups) {
        this.groups = groups;
    }

    public List<FacilityModel> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<FacilityModel> facilities) {
        this.facilities = facilities;
    }

    public LocationObservationCollectionModel getLocationObservationCollection() {
        return locationObservationCollection;
    }

    public void setLocationObservationCollection( LocationObservationCollectionModel locationObservationCollection) {
        this.locationObservationCollection = locationObservationCollection;
    }

    public List<URI> getOrganizationURIListOrEmpty() {
        if (getOrganizations() == null) {
            return Collections.emptyList();
        }

        return getOrganizations().stream()
                .map(OrganizationModel::getUri)
                .collect(Collectors.toList());
    }

    @Override
    public String[] getInstancePathSegments(SiteModel instance) {
        return new String[]{
                "site",
                instance.getName()
        };
    }
}
