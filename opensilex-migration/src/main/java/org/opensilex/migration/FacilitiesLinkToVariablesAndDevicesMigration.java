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
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.update.OpensilexModuleUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
        String dataCollectionName = DataDaoV2.COLLECTION_NAME;
        MongoDatabase db = mongodb.getDatabase();
        MongoCollection<DataModel> collection = db.getCollection(dataCollectionName, DataModel.class);

        try {
            sparql.startTransaction();
            mongodb.startTransaction();

            // 1 Get all facilities
            List<URI> allFacilityUris = sparql.searchURIs(sparql.getDefaultGraph(FacilityModel.class), FacilityModel.class, "en");
            // 2 Get all provenances and variables used in data that has for target these facilities
            //List<ProvenanceModel> provenances = dataLogic.searchUsedProvenances(null, allFacilityUris, null, null);
            //dataLogic.getUsedVariables(null, allFacilityUris, null, null);
            DataSearchFilter dataSearchFilter = new DataSearchFilter();
            dataSearchFilter.setUser(AccountModel.getSystemUser());
            dataSearchFilter.setTargets(allFacilityUris);
            dataSearchFilter.setPageSize(20);
            dataSearchFilter.setPage(0);
            /*var query = new MongoSearchQuery<DataModel, DataSearchFilter, DataGetSearchDTO>()
                    .setFilter(dataSearchFilter)
                    .setConvertFunction(model -> DataGetSearchDTO.getDtoFromModel(model, dateVariables))
            dataLogic.searchWithPagination(new MongoSearchQuery<>())*/
            List<DataModel> datas = dataDaoV2.searchWithPagination(dataSearchFilter).getList();
            List<FacilityModel> facilitiesToUpdate = dataLogic.handleExtractionOfFacilitiesToUpdate(datas);
            System.out.println("chahahaha");


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
