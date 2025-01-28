package org.opensilex.core.location.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.SOSA;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;

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

    public URI getCollectionURI(URI featureOfInterest) throws SPARQLException {
        SelectBuilder select = new SelectBuilder().addWhere(makeVar(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD), SOSA.hasFeatureOfInterest, SPARQLDeserializers.nodeURI(featureOfInterest));
        List<SPARQLResult> result = sparql.executeSelectQuery(select);

        return result.stream().map(x -> URI.create(x.getStringValue(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD))).findFirst().orElse(null);
    }

    public Map<URI, URI> getCollections(List<URI> featureOfInterests) throws SPARQLException {
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

    /**
     *
     * SELECT  *
     * WHERE
     * { ?rdfType (<http://www.w3.org/2000/01/rdf-schema#subClassOf>)* <http://www.opensilex.org/vocabulary/oeso#ScientificObject>.
     * ?uri a ?rdfType.
     * <p>
     * GRAPH  <http://opensilex.test/set/ObservationCollection> {
     * <p>
     * ?collection  sosa:hasFeatureOfInterest ?uri .
     * }
     * }
     */
    public Map<SPARQLNamedResourceModel, LocationObservationCollectionModel> getCollectionByType(URI rdfType) throws SPARQLException {
        //Variables
        Var collectionVar = makeVar(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD);
        Var typeVar = makeVar(SPARQLResourceModel.TYPE_FIELD);
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        Var nameVar = makeVar(SPARQLNamedResourceModel.NAME_FIELD);

        //Graph
        Node graphObservationCollection = SPARQLDeserializers.nodeURI(sparql.getDefaultGraphURI(LocationObservationCollectionModel.class));
        Node graphGlobalSO = sparql.getDefaultGraph(ScientificObjectModel.class);

        //Where clause
        WhereBuilder where = new WhereBuilder();
        //rdfType
        where.addWhere(new TriplePath(typeVar, Ontology.subClassAny, SPARQLDeserializers.nodeURI(rdfType)));
        //if rdtTYpe == SO : get only the global rdfType
        if (SPARQLDeserializers.compareURIs(rdfType, URI.create(Oeso.ScientificObject.getURI()))) {
            where.addGraph(graphGlobalSO, uriVar, RDF.type, typeVar);
            where.addGraph(graphGlobalSO, uriVar, RDFS.label.asNode(),nameVar);

        } else {
            where.addWhere(uriVar, RDF.type, typeVar);
            where.addWhere( uriVar, RDFS.label.asNode(),nameVar);
        }



        //collection
        where.addGraph(graphObservationCollection, collectionVar, SOSA.hasFeatureOfInterest.asNode(), uriVar);

        SelectBuilder select = new SelectBuilder().addWhere(where);

        List<SPARQLResult> results = sparql.executeSelectQuery(select);

        return results.stream().collect(Collectors.toMap(
                sparqlResult -> {
                    SPARQLNamedResourceModel resourceModel = new SPARQLNamedResourceModel();
                    resourceModel.setType(URI.create(sparqlResult.getStringValue(SPARQLResourceModel.TYPE_FIELD)));
                    resourceModel.setUri(URI.create(sparqlResult.getStringValue(SPARQLResourceModel.URI_FIELD)));
                    resourceModel.setName(sparqlResult.getStringValue(SPARQLNamedResourceModel.NAME_FIELD));
                    return resourceModel;
                },
                sparqlResult -> {
                    LocationObservationCollectionModel collectionModel = new LocationObservationCollectionModel();
                    collectionModel.setFeatureOfInterest(URI.create(sparqlResult.getStringValue(SPARQLResourceModel.URI_FIELD)));
                    collectionModel.setUri(URI.create(sparqlResult.getStringValue(LocationObservationCollectionModel.OBSERVATION_COLLECTION_FIELD)));
                    return collectionModel;
                }
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
