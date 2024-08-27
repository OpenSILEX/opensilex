package org.opensilex.core.location.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;

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
        return sparql.getByURI(LocationObservationCollectionModel.class,collectionURI, null);
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
