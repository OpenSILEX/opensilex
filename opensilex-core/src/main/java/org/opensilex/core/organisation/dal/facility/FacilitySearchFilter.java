/*******************************************************************************
 *                         FacilitySearchFilter.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 31/10/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.organisation.dal.facility;

import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.service.SparqlSearchFilter;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

/**
 * @author Valentin Rigolle
 */
public class FacilitySearchFilter extends SparqlSearchFilter {
    private UserModel user;
    private String pattern;
    private List<URI> facilities;
    private List<URI> organizations;

    @NotNull
    public UserModel getUser() {
        return user;
    }

    public FacilitySearchFilter setUser(UserModel user) {
        this.user = user;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public FacilitySearchFilter setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public List<URI> getFacilities() {
        return facilities;
    }

    public FacilitySearchFilter setFacilities(List<URI> facilities) {
        this.facilities = facilities;
        return this;
    }

    public List<URI> getOrganizations() {
        return organizations;
    }

    public FacilitySearchFilter setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
        return this;
    }
}
