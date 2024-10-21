/*
 * *****************************************************************************
 *                         OrganizationDagDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 04/09/2024 09:27
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.organisation.api;

import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceDagDTO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DAG (Directed Acyclic Graph) DTO specialised for {@link OrganizationModel} to return linked facilities, not only the parent and children.
 * @see org.opensilex.sparql.response.ResourceDagDTO
 */
public class OrganizationDagDTO extends ResourceDagDTO<OrganizationModel> {

    protected List<NamedResourceDTO> facilities;

    public List<NamedResourceDTO> getFacilities() { return facilities; }
    public void setFacilities(List<NamedResourceDTO> facilities) { this.facilities = facilities; }

    @Override
    public void fromModel(OrganizationModel model) {
        super.fromModel(model);

        if (Objects.nonNull(model.getFacilities())) {
            setFacilities(model.getFacilities().stream()
                    .map(NamedResourceDTO::getDTOFromModel)
                    .collect(Collectors.toList())
            );
        }
    }

    @Override
    public void fromModelChildren(OrganizationModel model) {
        super.fromModelChildren(model);

        if (Objects.nonNull(model.getFacilities())) {
            setFacilities(model.getFacilities().stream()
                    .map(NamedResourceDTO::getDTOFromModel)
                    .collect(Collectors.toList())
            );
        }
    }
}
