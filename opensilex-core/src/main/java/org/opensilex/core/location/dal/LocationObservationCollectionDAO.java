package org.opensilex.core.location.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.sparql.core.Var;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class LocationObservationCollectionDAO {
    private final SPARQLService sparql;

    //#region constructor
    public LocationObservationCollectionDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }
    //#endregion

    //#region public
    public URI create(LocationObservationCollectionModel locationObservationCollectionModel) throws Exception {
        sparql.create(locationObservationCollectionModel);
        return locationObservationCollectionModel.getUri();
    }

    public LocationObservationCollectionModel get(URI collectionURI) throws Exception {
        return sparql.getByURI(LocationObservationCollectionModel.class, collectionURI, null);
    }

    public URI getCollection(URI featureOfInterest) throws SPARQLException {
        SelectBuilder select = new SelectBuilder().addWhere(makeVar(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD), SOSA.hasFeatureOfInterest, SPARQLDeserializers.nodeURI(featureOfInterest));
        List<SPARQLResult> result = sparql.executeSelectQuery(select);

        return result.stream().map(x -> URI.create(x.getStringValue(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD))).findFirst().orElse(null);
    }

    public Map<URI,URI> getCollections(List<URI> featureOfInterests) throws SPARQLException {
        Var featuresVar = makeVar(LocationObservationModel.FEATURE_OF_INTEREST_FIELD);

        SelectBuilder select = new SelectBuilder()
                .addWhere(makeVar(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD), SOSA.hasFeatureOfInterest, featuresVar)
                .addFilter(SPARQLQueryHelper.inURIFilter(featuresVar, featureOfInterests));

        List<SPARQLResult> results = sparql.executeSelectQuery(select);

        return results.stream().collect(Collectors.toMap(
                sparqlResult -> URI.create(sparqlResult.getStringValue(LocationObservationModel.FEATURE_OF_INTEREST_FIELD)),
                sparqlResult -> URI.create(sparqlResult.getStringValue(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD))
        ));
    }

    public void delete(URI collectionURI) throws Exception {
        sparql.delete(LocationObservationCollectionModel.class, collectionURI);
    }

    public List<SPARQLResult> validateUniquenessObservationCollection(URI featureOfInterest) throws SPARQLException {
        SelectBuilder select = new SelectBuilder().addWhere(makeVar(LocationObservationCollectionModel.GRAPH), SOSA.hasFeatureOfInterest, SPARQLDeserializers.nodeURI(featureOfInterest));

        return sparql.executeSelectQuery(select);
    }
    //#endregion


}
