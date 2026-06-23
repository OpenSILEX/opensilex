package org.opensilex.migration.one_point_five_ALL;

import com.mongodb.client.model.Aggregates;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.bson.Document;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class FacilitiesLinkToVariablesAndDevicesMigration {

    private final SPARQLService sparql;
    private final MongoDBService mongodb;
    private final Logger logger;

    protected static String DESCRIPTION = "Gets all facilities, to then fetch all data on these facilities, to finally be able to add the facilities-variables and facilities-devices links.";

    private static final int BATCH_SIZE = 20000;

    public FacilitiesLinkToVariablesAndDevicesMigration(SPARQLService sparql, MongoDBService mongodb, Logger logger) {
        this.sparql = sparql;
        this.mongodb = mongodb;
        this.logger = logger;
    }

    /**
     * Checks if this migration was most likely already run. Does this by checking if any Facilities have devices or variables.
     * If they do not then we suppose that migration has not been done. It would technically be possible that it could have been done if
     * no data has a Facility as target, but the migration just wouldn't do anything in this case.
     * @return true if any Facility has a device or variable.
     */
    protected boolean wasMigrationPreviouslyRun() throws SPARQLException {
        AskBuilder hasDeviceSelect = new AskBuilder();
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var deviceVar = makeVar("device");
        Var typeVar = makeVar("type");

        hasDeviceSelect.addWhere(typeVar, Ontology.subClassAny, Oeso.Facility);
        hasDeviceSelect.addWhere(uriVar, RDF.type, typeVar);
        hasDeviceSelect.addWhere(uriVar, Oeso.hasDevice, deviceVar);

        boolean aFacilityDoesHaveDevices = sparql.executeAskQuery(hasDeviceSelect);
        //If we have OS locations, no need to check for devices, we can leave
        if(aFacilityDoesHaveDevices){
            return true;
        }

        //Else do same check for devices
        AskBuilder hasVariableSelect = new AskBuilder();
        Var variableVar = makeVar("variable");

        hasVariableSelect.addWhere(typeVar, Ontology.subClassAny, Oeso.Facility);
        hasVariableSelect.addWhere(uriVar, RDF.type, typeVar);
        hasVariableSelect.addWhere(uriVar, Oeso.hasVariable, variableVar);

        return sparql.executeAskQuery(hasVariableSelect);
    }

    private List<FacilityModel> mergeFacilitiesToUpdateList(List<FacilityModel> newFacilitiesToUpdate, List<FacilityModel> previouslyCalculatedFacilitiesToUpdate){
        List<FacilityModel> newOnesToAddToResult = new ArrayList<>();
        for(FacilityModel nextFacility : newFacilitiesToUpdate){
            FacilityModel matchedFacility =
                    previouslyCalculatedFacilitiesToUpdate.stream()
                            .filter(e -> SPARQLDeserializers.compareURIs(e.getUri(), nextFacility.getUri()))
                            .findFirst()
                            .orElse(null);
            if(matchedFacility == null){
                newOnesToAddToResult.add(nextFacility);
            }else{
                //Combine the variables and devices lists
                List<VariableModel> previousVars = matchedFacility.getVariables();
                List<VariableModel> newVars = nextFacility.getVariables();
                for(VariableModel newVar : newVars){
                    if(previousVars.stream().noneMatch(e -> SPARQLDeserializers.compareURIs(e.getUri(), newVar.getUri()))){
                        previousVars.add(newVar);
                    }
                }
                List<DeviceModel> previousDevices = matchedFacility.getDevices();
                List<DeviceModel> newDevices = nextFacility.getDevices();
                for(DeviceModel newDevice : newDevices){
                    if(previousDevices.stream().noneMatch(e -> SPARQLDeserializers.compareURIs(e.getUri(), newDevice.getUri()))){
                        previousDevices.add(newDevice);
                    }
                }
            }

        }
        previouslyCalculatedFacilitiesToUpdate.addAll(newOnesToAddToResult);
        return previouslyCalculatedFacilitiesToUpdate;

    }

    protected void execute() throws Exception {
        testAggregation();
        return;
//        FacilityDAO facilityDAO = new FacilityDAO(sparql);
//        DataLogic dataLogic = new DataLogic(sparql, mongodb, null, AccountModel.getSystemUser());
//        DataDaoV2 dataDaoV2 = new DataDaoV2(sparql, mongodb, null);
//
//        try {
//
//            // 1 Get all facilities
//            List<URI> allFacilityUris = sparql.searchURIs(sparql.getDefaultGraph(FacilityModel.class), FacilityModel.class, "en");
//
//            if(CollectionUtils.isEmpty(allFacilityUris)){
//                return;
//            }
//
//            // 2 Get all data that has for target these facilities
//            List<FacilityModel> facilitiesToUpdate = new ArrayList<>();
//
//            DataSearchFilter dataSearchFilter = new DataSearchFilter();
//            dataSearchFilter.setUser(AccountModel.getSystemUser());
//            dataSearchFilter.setTargets(allFacilityUris);
//            dataSearchFilter.setPageSize(BATCH_SIZE);
//
//            boolean done = false;
//            int page = 0;
//            while(!done){
//                dataSearchFilter.setPage(page);
//                ListWithPagination<DataModel> nextPage = dataDaoV2.searchWithPagination(dataSearchFilter);
//                if(nextPage.getList().size() < BATCH_SIZE) {
//                    done = true;
//                }
//                if (logger.isDebugEnabled()) {
//                    var dbgPage = 1 + page;
//                    var dbgTotalPage = 1 + nextPage.getTotal() / BATCH_SIZE;
//                    var dbgPercent = 100 * (float) dbgPage / (float) dbgTotalPage;
//                    logger.debug(String.format("Done : page %s of %s (progress: %.1f %%)", dbgPage, dbgTotalPage, dbgPercent));
//                    logger.debug("Sleeping to avoid stressing RDF4J");
//                }
//                Thread.sleep(1000);
//                //Save facilities into list instead of data (there should be less and it should take up less memory)
//                List<FacilityModel> nextFacilitiesToUpdate = dataLogic.getFacilitiesToUpdate(nextPage.getList());
//                facilitiesToUpdate = mergeFacilitiesToUpdateList(nextFacilitiesToUpdate, facilitiesToUpdate);
//
//                page++;
//            }
//
//            //3 Extract facilities that need updating with he same function that's used during data import, then update the facilities
//            facilityDAO.updateMany(facilitiesToUpdate);
//
//        } catch (Exception e){
//            logger.warn("Something went wrong in the FacilitiesLinkToVariablesAndDevicesMigration part of the migration!");
//            throw e;
//        }
    }

    private void testAggregation() {
        var pipeline = List.of(
//                Aggregates.skip(page * pageSize),
//                Aggregates.limit(pageSize),
                Aggregates.project(new Document(Map.of(
                        "t", "$target",
                        "v", "$variable",
                        "p", "$provenance.provWasAssociatedWith"
                )))
                );
        var collection = mongodb.getServiceV2().getDatabase().getCollection("data");
        var batch = new ArrayList<TestAggregationResult>(BATCH_SIZE);
        var currentIndex = 0;
        var total = collection.countDocuments();
        try (var cursor = collection.aggregate(pipeline).cursor()) {
            while (currentIndex < total) {
                batch.clear();
                while (cursor.hasNext() && batch.size() < BATCH_SIZE) {
                    var document = cursor.next();
                    var target = document.getString("t");
                    var variable = document.getString("v");
                    var provEntity = document.getList("p", Document.class);
                    batch.add(new TestAggregationResult(
                            target,
                            variable,
                            provEntity.stream()
                                    .filter(d -> d.get("uri") != null && d.get("type") != null)
                                    .map(d -> new ProvEntityModel(URI.create(d.getString("uri")), URI.create(d.getString("type")))).toList()
                    ));
                    currentIndex += 1;
                }
                logger.info("First document of batch : {} - {} - {}", batch.get(0).getTarget(), batch.get(0).getVariable(), batch.get(0).getProvEntities().get(0).getUri());
                logger.info("Number of elements in batch : {}", batch.size());
                logger.info(String.format("Progress : %s / %s (%.1f %%)", currentIndex, total, 100.f * currentIndex / total));
            }
        }
    }

}

class TestAggregationResult {
    private final String target;
    private final String variable;
    private final List<ProvEntityModel> provEntities;

    public TestAggregationResult(String target, String variable, List<ProvEntityModel> provEntities) {
        this.target = target;
        this.variable = variable;
        this.provEntities = provEntities;
    }

    public String getTarget() {
        return target;
    }

    public String getVariable() {
        return variable;
    }

    public List<ProvEntityModel> getProvEntities() {
        return provEntities;
    }
}