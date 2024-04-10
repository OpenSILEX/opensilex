/*******************************************************************************
 *                         SiteSearchFilter.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 31/10/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.organisation.dal.site;

import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SearchFilter;

import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * @author Valentin Rigolle
 */
public class SiteSearchFilter extends SearchFilter {
    private String namePattern;
    private List<URI> sites;
    private List<URI> organizations;
    private URI facility;
    private AccountModel user;
    /**
     * If set to true, the site search method will not perform a query to fetch the organizations accessible to the
     * user. Instead, it will use the {@link #userOrganizations} search field as the list of accessible organizations.
     */
    private boolean skipUserOrganizationFetch = false;
    private List<URI> userOrganizations;

    public SiteSearchFilter() {
        super();
    }

    public String getNamePattern() {
        return namePattern;
    }

    public SiteSearchFilter setNamePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }

    public List<URI> getSites() {
        return sites;
    }

    public SiteSearchFilter setSites(List<URI> sites) {
        this.sites = sites;
        return this;
    }

    public List<URI> getOrganizations() {
        return organizations;
    }

    public SiteSearchFilter setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
        return this;
    }

    public URI getFacility() {
        return facility;
    }

    public SiteSearchFilter setFacility(URI facility) {
        this.facility = facility;
        return this;
    }

    @NotNull
    public AccountModel getUser() {
        return user;
    }

    public SiteSearchFilter setUser(AccountModel user) {
        this.user = user;
        return this;
    }

    public boolean getSkipUserOrganizationFetch() {
        return skipUserOrganizationFetch;
    }

    public SiteSearchFilter setSkipUserOrganizationFetch(boolean skipUserOrganizationFetch) {
        this.skipUserOrganizationFetch = skipUserOrganizationFetch;
        return this;
    }

    public List<URI> getUserOrganizations() {
        return userOrganizations;
    }

    public SiteSearchFilter setUserOrganizations(List<URI> userOrganizations) {
        this.userOrganizations = userOrganizations;
        return this;
    }

    @Override
    public void validate() throws IllegalArgumentException, InvocationTargetException, IllegalAccessException {
        super.validate();

        // The fetching of user organizations can only be skipped iff a list of user organizations is provided with
        // the filter. This list can be empty in the case where the user has access to no organization.
        if (getSkipUserOrganizationFetch() && Objects.isNull(getUserOrganizations())) {
            throw new IllegalArgumentException("`skipUserOrganizationFetch` requires `userOrganizations` to be defined");
        }
    }
}
