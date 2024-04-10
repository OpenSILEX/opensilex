package org.opensilex.core.organisation.dal;

import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SearchFilter;

import java.net.URI;
import java.util.List;

public class OrganizationSearchFilter extends SearchFilter {

    private String nameFilter;
    private List<URI> restrictedOrganizations;
    private AccountModel user;
    private URI facilityURI;
    private URI directChildURI;

    public OrganizationSearchFilter() {
        super();
    }

    public String getNameFilter() {
        return nameFilter;
    }

    public OrganizationSearchFilter setNameFilter(String nameFilter) {
        this.nameFilter = nameFilter;
        return this;
    }

    public List<URI> getRestrictedOrganizations() {
        return restrictedOrganizations;
    }

    public OrganizationSearchFilter setRestrictedOrganizations(List<URI> restrictedOrganizations) {
        this.restrictedOrganizations = restrictedOrganizations;
        return this;
    }

    public AccountModel getUser() {
        return user;
    }

    public OrganizationSearchFilter setUser(AccountModel user) {
        this.user = user;
        return this;
    }

    public URI getFacilityURI() {
        return facilityURI;
    }

    public OrganizationSearchFilter setFacilityURI(URI facilityURI) {
        this.facilityURI = facilityURI;
        return this;
    }

    public URI getDirectChildURI() {
        return directChildURI;
    }

    public OrganizationSearchFilter setDirectChildURI(URI directChildURI) {
        this.directChildURI = directChildURI;
        return this;
    }
}
