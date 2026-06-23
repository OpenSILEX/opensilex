package org.opensilex.migration.one_point_five_ALL;

import com.mongodb.client.model.Aggregates;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.bson.Document;
import org.opensilex.core.data.bll.DataLogic;
import org.opensilex.core.data.bll.MinimalData;
import org.opensilex.core.data.dal.ProvEntityModel;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
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
     *
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
        if (aFacilityDoesHaveDevices) {
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

    private void mergeFacilitiesToUpdateList(List<FacilityModel> newFacilitiesToUpdate, List<FacilityModel> allFacilities) {
        List<FacilityModel> newOnesToAddToResult = new ArrayList<>();
        for (FacilityModel nextFacility : newFacilitiesToUpdate) {
            FacilityModel matchedFacility =
                    allFacilities.stream()
                            .filter(e -> SPARQLDeserializers.compareURIs(e.getUri(), nextFacility.getUri()))
                            .findFirst()
                            .orElse(null);
            if (matchedFacility == null) {
                newOnesToAddToResult.add(nextFacility);
            } else {
                //Combine the variables and devices lists
                List<VariableModel> previousVars = matchedFacility.getVariables();
                List<VariableModel> newVars = nextFacility.getVariables();
                for (VariableModel newVar : newVars) {
                    if (previousVars.stream().noneMatch(e -> SPARQLDeserializers.compareURIs(e.getUri(), newVar.getUri()))) {
                        previousVars.add(newVar);
                    }
                }
                List<DeviceModel> previousDevices = matchedFacility.getDevices();
                List<DeviceModel> newDevices = nextFacility.getDevices();
                for (DeviceModel newDevice : newDevices) {
                    if (previousDevices.stream().noneMatch(e -> SPARQLDeserializers.compareURIs(e.getUri(), newDevice.getUri()))) {
                        previousDevices.add(newDevice);
                    }
                }
            }

        }
        allFacilities.addAll(newOnesToAddToResult);
    }

    protected void execute() throws Exception {
        updateFacilities();
    }

    private void updateFacilities() throws Exception {
        var dataLogic = new DataLogic(sparql, mongodb, null, AccountModel.getSystemUser());
        var facilityDao = new FacilityDAO(sparql);
        List<FacilityModel> facilitiesToUpdate = new ArrayList<FacilityModel>();

        var pipeline = List.of(Aggregates.project(new Document(Map.of(
                "t", "$target",
                "v", "$variable",
                "p", "$provenance.provWasAssociatedWith"
        ))));
        var collection = mongodb.getServiceV2().getDatabase().getCollection("data");
        var batch = new ArrayList<MinimalData>(BATCH_SIZE);
        var currentIndex = 0;
        var total = collection.countDocuments();
        try (var cursor = collection.aggregate(pipeline).cursor()) {
            while (currentIndex < total) {
                batch.clear();
                while (cursor.hasNext() && batch.size() < BATCH_SIZE) {
                    var document = cursor.next();
                    var provEntity = document.getList("p", Document.class);
                    batch.add(new MinimalData(
                            URI.create(document.getString("t")),
                            URI.create(document.getString("v")),
                            provEntity.stream()
                                    .filter(d -> d.get("uri") != null && d.get("type") != null)
                                    .map(d -> new ProvEntityModel(URI.create(d.getString("uri")), URI.create(d.getString("type")))).toList()
                    ));
                    currentIndex += 1;
                }
                var nextFacilitiesToUpdate = dataLogic.getFacilitiesToUpdate(batch);
                mergeFacilitiesToUpdateList(nextFacilitiesToUpdate, facilitiesToUpdate);
                logger.info(String.format("Progress : %s / %s (%.1f %%)", currentIndex, total, 100.f * currentIndex / total));
            }
        }
        facilityDao.updateMany(facilitiesToUpdate);
    }
}