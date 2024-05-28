/*
 * *****************************************************************************
 *                         GermplasmLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 28/05/2024 16:23
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.olga.bll;

import org.brapi.client.v2.ApiResponse;
import org.brapi.client.v2.model.exceptions.ApiException;
import org.brapi.v2.model.BrAPIPagination;
import org.brapi.v2.model.BrAPIStatus;
import org.brapi.v2.model.germ.BrAPIGermplasm;
import org.brapi.v2.model.germ.response.BrAPIGermplasmSingleResponse;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.olga.OlgaModule;
import org.opensilex.olga.dal.GermplasmClient;
import org.opensilex.olga.dal.GermplasmDAO;
import org.opensilex.olga.dal.GermplasmModel;
import org.opensilex.olga.model.GermplasmDTO;

import java.util.List;
import java.util.stream.Collectors;

public class GermplasmLogic {

    private final GermplasmDAO germplasmDAO;
    private final GermplasmClient germplasmClient;

    public GermplasmLogic(MongoDBServiceV2 mongodb, OlgaModule olgaModule) {
        this.germplasmDAO = new GermplasmDAO(mongodb);
        this.germplasmClient = new GermplasmClient(olgaModule);
    }


    public void updateGermplasms(List<GermplasmDTO> germplasmDTOs) throws Exception {
        List<GermplasmModel> germplasmModels = germplasmDTOs.stream().map(GermplasmModel::fromDTO).collect(Collectors.toList());

        germplasmDAO.create(germplasmModels);
    }

    public List<GermplasmDTO> searchGermplasm(String germplasmName) {
        return germplasmDAO.searchGermplasms(germplasmName).stream().map(GermplasmModel::toDTO).collect(Collectors.toList());
    }

    public void harvestOlgaGermplasms() throws Exception {
        List<BrAPIGermplasm> germplasms = germplasmClient.harvestOlgaGermplasms();
        updateGermplasms(
                germplasms.stream().map(GermplasmDTO::fromBrapiGermplasm).collect(Collectors.toList())
        );
    }

    public GermplasmDetailResult getOpensilexGermplasmDetail(String germplasmDbId) throws ApiException {
        ApiResponse<BrAPIGermplasmSingleResponse> brapiResponse = germplasmClient.getBrAPIGermplasm(germplasmDbId);

        BrAPIGermplasm brapiDTO = brapiResponse.getBody().getResult();
        var opensilexModel = new org.opensilex.core.germplasm.dal.GermplasmModel();
        opensilexModel.setName(brapiDTO.getGermplasmName());

        return new GermplasmDetailResult()
                .setGermplasmModel(opensilexModel)
                .setResponsePagination(brapiResponse.getBody().getMetadata().getPagination())
                .setResponseStatus(brapiResponse.getBody().getMetadata().getStatus());
    }

    public static class GermplasmDetailResult {
        private org.opensilex.core.germplasm.dal.GermplasmModel germplasmModel;
        private BrAPIPagination responsePagination;
        private List<BrAPIStatus> responseStatus;

        public org.opensilex.core.germplasm.dal.GermplasmModel getGermplasmModel() {
            return germplasmModel;
        }

        public GermplasmDetailResult setGermplasmModel(org.opensilex.core.germplasm.dal.GermplasmModel germplasmModel) {
            this.germplasmModel = germplasmModel;
            return this;
        }

        public BrAPIPagination getResponsePagination() {
            return responsePagination;
        }

        public GermplasmDetailResult setResponsePagination(BrAPIPagination responsePagination) {
            this.responsePagination = responsePagination;
            return this;
        }

        public List<BrAPIStatus> getResponseStatus() {
            return responseStatus;
        }

        public GermplasmDetailResult setResponseStatus(List<BrAPIStatus> responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }
    }
}
