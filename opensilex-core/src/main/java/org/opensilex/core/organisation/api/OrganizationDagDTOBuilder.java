/*
 * *****************************************************************************
 *                         OrganizationDagDTOBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 30/08/2024 09:38
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

package org.opensilex.core.organisation.api;

import org.opensilex.core.organisation.dal.OrganizationModel;
import org.opensilex.sparql.response.ResourceDagDTO;
import org.opensilex.sparql.response.ResourceDagDTOBuilder;

import java.util.List;

/**
 * needed to build the OrganizationDagDTO rather than ResourceDagDTO.
 * @see ResourceDagDTOBuilder
 */
public class OrganizationDagDTOBuilder extends ResourceDagDTOBuilder<OrganizationModel> {

    public OrganizationDagDTOBuilder(List<OrganizationModel> dagModelList) {
        super(dagModelList);
    }

    @Override
    protected ResourceDagDTO<OrganizationModel> instanciateDto() {
        return new OrganizationDagDTO();
    }
}
