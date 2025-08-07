package org.opensilex.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.opensilex.OpenSilex;
import org.opensilex.core.data.api.DataGetSearchDTO;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataDaoV2;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataSearchFilter;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.organisation.dal.facility.FacilityAddressModel;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.provenance.dal.ProvenanceModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FacilitiesLinkToVariablesAndDevicesMigration implements OpenSilexModuleUpdate {

    private OpenSilex opensilex;
    private SPARQLService sparql;
    private MongoDBService mongodb;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String getDescription() {
        return "Gets all facilities, to then fetch all data on these facilities, to finally be able to add the facilities-variables and facilities-devices links.";
    }

    @Override
    public void setOpensilex(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public OffsetDateTime getDate() {
        return OffsetDateTime.now();
    }

    @Override
    public void execute() throws OpensilexModuleUpdateException {

        SPARQLServiceFactory factory = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparql = factory.provide();
        mongodb = opensilex.getServiceInstance(MongoDBService.DEFAULT_SERVICE, MongoDBService.class);

        FacilityDAO facilityDAO = new FacilityDAO(sparql);
        DataLogic dataLogic = new DataLogic(sparql, mongodb, null, AccountModel.getSystemUser());
        DataDaoV2 dataDaoV2 = new DataDaoV2(sparql, mongodb, null);

        try {
            sparql.startTransaction();
            mongodb.startTransaction();

            // 1 Get all facilities

            List<URI> allFacilityUris = sparql.searchURIs(sparql.getDefaultGraph(FacilityModel.class), FacilityModel.class, "en");

            // 2 Get all data that has for target these facilities

            List<DataModel> dataWithFacilitiesAsTargets = new ArrayList<>();

            DataSearchFilter dataSearchFilter = new DataSearchFilter();
            dataSearchFilter.setUser(AccountModel.getSystemUser());
            dataSearchFilter.setTargets(allFacilityUris);
            dataSearchFilter.setPageSize(5000);

            boolean done = false;
            int page = 0;
            while(!done){
                dataSearchFilter.setPage(page);
                ListWithPagination<DataModel> nextPage = dataDaoV2.searchWithPagination(dataSearchFilter);
                if(nextPage.getTotal() < 5000 || nextPage.getTotal() == 0) {
                    done = true;
                }
                dataWithFacilitiesAsTargets.addAll(nextPage.getList());

                page++;
            }

            //3 Extract facilities that need updating witht he same function that's used during data import, then update the facilities
            List<FacilityModel> facilitiesToUpdate = dataLogic.handleExtractionOfFacilitiesToUpdate(dataWithFacilitiesAsTargets);
            facilityDAO.updateMany(facilitiesToUpdate);

            sparql.commitTransaction();
            mongodb.commitTransaction();
            logger.info("Migration successfully completed");

        } catch (Exception e){
            try {
                sparql.rollbackTransaction();
                mongodb.rollbackTransaction();
                logger.error("Error while migrating facilities. No changes were saved on the databases", e);
            } catch (Exception exception) {
                throw new OpensilexModuleUpdateException("Error while migrating facilities. No changes were saved on the databases", exception);
            }
        }
    }

}
