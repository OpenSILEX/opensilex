/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import org.opensilex.core.ontology.dal.CSVValidationModel;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.URIGenerator;

/**
 *
 * @author vmigot
 */
public class ScientificObjectDAO {

    private final SPARQLService sparql;

    public ScientificObjectDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public SPARQLPartialTreeListModel<ExperimentalObjectModel> searchTreeByExperiment(URI experimentURI, URI parentURI, int maxChild, int maxDepth, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.searchPartialResourceTree(
                experimentGraph,
                ExperimentalObjectModel.class,
                currentUser.getLanguage(),
                ScientificObjectModel.PARENT_FIELD,
                Oeso.isPartOf,
                parentURI,
                maxChild,
                maxDepth,
                null
        );
    }

    public List<ScientificObjectModel> searchByExperiment(URI experimentURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.search(experimentGraph, ScientificObjectModel.class, currentUser.getLanguage());
    }

    public List<ScientificObjectModel> searchByURIs(URI experimentURI, List<URI> objectsURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);

        List<URI> uniqueObjectsUri = objectsURI.stream()
                .distinct()
                .collect(Collectors.toList());
        return sparql.getListByURIs(experimentGraph, ScientificObjectModel.class, uniqueObjectsUri, currentUser.getLanguage());
    }

    public List<ScientificObjectModel> searchChildrenByExperiment(URI experimentURI, URI parentURI, Integer offset, Integer limit, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.search(
                experimentGraph,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    if (parentURI != null) {
                        select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
                    } else {
                        Triple parentTriple = new Triple(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf.asNode(), makeVar("parentURI"));
                        select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(parentTriple)));
                    }
                },
                null,
                offset,
                limit);
    }

    public CSVValidationModel validateCSV(URI xpURI, URI soType, InputStream file, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        HashMap<String, BiConsumer<CSVCell, CSVValidationModel>> customValidators = new HashMap<>();

        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpURI, currentUser.getLanguage());

        List<String> factorLevelURIs = new ArrayList<>();
        for (FactorModel factor : xp.getFactors()) {
            for (FactorLevelModel factorLevel : factor.getFactorLevels()) {
                factorLevelURIs.add(SPARQLDeserializers.getExpandedURI(factorLevel.getUri()));
            }
        }

        List<String> germplasmURIs = new ArrayList<>();
        for (SpeciesModel germplasm : xp.getSpecies()) {
            germplasmURIs.add(SPARQLDeserializers.getExpandedURI(germplasm.getUri()));
        }

        customValidators.put(Oeso.hasFactorLevel.toString(), (cell, csvErrors) -> {
            try {
                String factorLevelURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                if (!factorLevelURIs.contains(factorLevelURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        customValidators.put(Oeso.hasGermplasm.toString(), (cell, csvErrors) -> {
            try {
                String germplasmURI = SPARQLDeserializers.getExpandedURI(new URI(cell.getValue()));
                if (!germplasmURIs.contains(germplasmURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        return ontologyDAO.validateCSV(xpURI, soType, file, currentUser, customValidators, new ScientificObjectExperimentURIGenerator(xpURI));
    }

    public URI create(URI xpURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        SPARQLResourceModel object = initObject(xpURI, soType, name, relations, currentUser);

        if (objectURI == null) {
            ScientificObjectExperimentURIGenerator uriGenerator = new ScientificObjectExperimentURIGenerator(xpURI);
            int retry = 0;
            objectURI = uriGenerator.generateURI(xpURI.toString(), name, retry);
            while (sparql.uriExists(SPARQLDeserializers.nodeURI(xpURI), objectURI)) {
                retry++;
                objectURI = uriGenerator.generateURI(xpURI.toString(), name, retry);
            }
        }
        object.setUri(objectURI);

        sparql.create(SPARQLDeserializers.nodeURI(xpURI), object);

        return object.getUri();
    }

    public URI update(URI xpURI, URI soType, URI objectURI, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        SPARQLResourceModel object = initObject(xpURI, soType, name, relations, currentUser);
        object.setUri(objectURI);

        Node xpGraphNode = SPARQLDeserializers.nodeURI(xpURI);

        List<URI> childrenURIs = sparql.searchURIs(
                xpGraphNode,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                (select) -> {
                    select.addWhere(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf, SPARQLDeserializers.nodeURI(objectURI));
                });
        try {
            sparql.startTransaction();
            sparql.deleteByURI(xpGraphNode, objectURI);
            sparql.create(xpGraphNode, object);
            if (childrenURIs.size() > 0) {
                sparql.insertPrimitive(xpGraphNode, childrenURIs, Oeso.isPartOf, objectURI);
            }
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction(ex);
        }

        return object.getUri();
    }

    private SPARQLResourceModel initObject(URI xpURI, URI soType, String name, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, currentUser.getLanguage());

        SPARQLResourceModel object = new SPARQLResourceModel();
        object.setType(soType);

        for (RDFObjectRelationDTO relation : relations) {
            if (!ontologyDAO.validateObjectValue(xpURI, model, relation.getProperty(), relation.getValue(), object)) {
                throw new InvalidValueException("Invalid relation value for " + relation.getProperty().toString() + " => " + relation.getValue());
            }
        }

        object.addRelation(xpURI, new URI(RDFS.label.getURI()), String.class, name);

        return object;
    }

    public ExperimentalObjectModel getByURIAndExperiment(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        return sparql.getByURI(SPARQLDeserializers.nodeURI(xpURI), ExperimentalObjectModel.class, objectURI, currentUser.getLanguage());
    }

    public void delete(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        sparql.deleteByURI(SPARQLDeserializers.nodeURI(xpURI), objectURI);
    }

    private class ScientificObjectExperimentURIGenerator implements URIGenerator<String> {

        private final URI experimentURI;

        public ScientificObjectExperimentURIGenerator(URI experimentURI) {
            this.experimentURI = experimentURI;
        }

        @Override
        public URI generateURI(String prefix, String name, int retryCount) throws Exception {
            String baseURI = null;
            if (name != null) {
                baseURI = experimentURI.toString() + "/so-" + URIGenerator.normalize(name);
            } else {
                baseURI = experimentURI.toString() + "/so-" + randomAlphaNumeric(7);
            }

            if (retryCount > 0) {
                baseURI += "-" + retryCount;
            }

            return new URI(baseURI);
        }

    }

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }

        return builder.toString().toLowerCase();
    }

}
