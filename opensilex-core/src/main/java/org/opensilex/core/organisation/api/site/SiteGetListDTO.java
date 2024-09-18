/*******************************************************************************
 *                         SiteGetListDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 04/05/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.core.organisation.api.site;

import org.opensilex.core.organisation.dal.site.SiteModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simplified DTO for retrieving a list of Sites. The {@link #organizations} field is a list of {@link URI}s instead
 * of a list of models.
 *
 * @author Valentin Rigolle
 */
public class SiteGetListDTO extends SiteDTO {

    protected SiteAddressDTO address;

    protected String description;

    protected List<URI> organizations;

    protected List<NamedResourceDTO> facilities;

    public SiteAddressDTO getAddress() {
        return address;
    }
    public void setAddress(SiteAddressDTO address) {
        this.address = address;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<URI> getOrganizations() {
        return organizations;
    }
    public void setOrganizations(List<URI> organizations) {
        this.organizations = organizations;
    }

    public List<NamedResourceDTO> getFacilities() { return facilities; }
    public void setFacilities(List<NamedResourceDTO> facilities) { this.facilities = facilities; }

    @Override
    public void fromModel(SiteModel model) {
        super.fromModel(model);

        if (Objects.nonNull(model.getOrganizations())) {
            setOrganizations(model.getOrganizations()
                    .stream().map(SPARQLResourceModel::getUri)
                    .collect(Collectors.toList())
            );
        }

        if (Objects.nonNull(model.getAddress())) {
            SiteAddressDTO addressDTO = new SiteAddressDTO();
            addressDTO.fromModel(model.getAddress());
            setAddress(addressDTO);
        }

        setDescription(model.getDescription());

        if (Objects.nonNull(model.getFacilities())) {
            setFacilities(model.getFacilities().stream()
                    .map(NamedResourceDTO::getDTOFromModel)
                    .collect(Collectors.toList())
            );
        }
    }
}
