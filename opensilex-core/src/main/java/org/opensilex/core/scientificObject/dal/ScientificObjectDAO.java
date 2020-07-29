/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import org.opensilex.core.ontology.dal.CSVValidationModel;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
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
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeListModel;
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

    public SPARQLPartialTreeListModel<ScientificObjectModel> searchTreeByExperiment(URI experimentURI, URI parentURI, int maxChild, int maxDepth, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.searchPartialResourceTree(
                experimentGraph,
                ScientificObjectModel.class,
                currentUser.getLanguage(),
                ScientificObjectModel.PARENT_FIELD,
                Oeso.isPartOf,
                parentURI,
                maxChild,
                maxDepth,
                null);
    }

    public List<ScientificObjectModel> searchChildrenByExperiment(URI experimentURI, URI parentURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(experimentURI, currentUser);

        Node experimentGraph = SPARQLDeserializers.nodeURI(experimentURI);
        return sparql.search(experimentGraph, ScientificObjectModel.class, currentUser.getLanguage(), (select) -> {
            if (parentURI != null) {
                select.addWhere(ScientificObjectModel.URI_FIELD, Oeso.isPartOf, SPARQLDeserializers.nodeURI(parentURI));
            } else {
                Triple parentTriple = new Triple(makeVar(ScientificObjectModel.URI_FIELD), Oeso.isPartOf.asNode(), makeVar("parentURI"));
                select.addFilter(SPARQLQueryHelper.getExprFactory().notexists(new WhereBuilder().addWhere(parentTriple)));
            }
        });
    }

    public CSVValidationModel validateCSV(URI xpURI, URI soType, File file, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);

        HashMap<Property, BiConsumer<CSVCell, CSVValidationModel>> customValidators = new HashMap<>();

        ExperimentModel xp = sparql.getByURI(ExperimentModel.class, xpURI, currentUser.getLanguage());

        List<URI> factorLevelURIs = new ArrayList<>();
        for (FactorModel factor : xp.getFactors()) {
            for (FactorLevelModel factorLevel : factor.getFactorLevels()) {
                factorLevelURIs.add(factorLevel.getUri());
            }
        }

        List<URI> germplasmURIs = new ArrayList<>();
        for (SpeciesModel germplasm : xp.getSpecies()) {
            germplasmURIs.add(germplasm.getUri());
        }

        customValidators.put(Oeso.hasFactorLevel, (cell, csvErrors) -> {
            try {
                URI factorLevelURI = new URI(cell.getValue());
                if (!factorLevelURIs.contains(factorLevelURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        customValidators.put(Oeso.hasGermplasm, (cell, csvErrors) -> {
            try {
                URI germplasmURI = new URI(cell.getValue());
                if (!germplasmURIs.contains(germplasmURI)) {
                    csvErrors.addInvalidValueError(cell);
                }
            } catch (URISyntaxException ex) {
                csvErrors.addInvalidURIError(cell);
            }
        });

        return ontologyDAO.validateCSV(xpURI, soType, file, currentUser, customValidators, new ScientificObjectExperimentURIGenerator(xpURI));
    }

    public URI create(URI xpURI, URI soType, URI objectURI, List<RDFObjectRelationDTO> relations, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ScientificObjectClassModel model = ontologyDAO.getClassModel(soType, ScientificObjectClassModel.class, currentUser.getLanguage());

        SPARQLResourceModel object = new SPARQLResourceModel();
        object.setType(soType);
        object.setUri(objectURI);

        boolean isValid = false;
        if (relations.size() != model.getRestrictions().size()) {
            isValid = true;
            for (RDFObjectRelationDTO relation : relations) {
                isValid = isValid && ontologyDAO.validateObjectValue(xpURI, model, relation.getProperty(), relation.getValue(), object);
                if (!isValid) {
                    break;
                }
            }
        }

        if (isValid) {
            sparql.create(SPARQLDeserializers.nodeURI(xpURI), object);
        }

        return object.getUri();
    }

    public ScientificObjectModel getByURIAndExperiment(URI xpURI, URI objectURI, UserModel currentUser) throws Exception {
        ExperimentDAO xpDAO = new ExperimentDAO(sparql);
        xpDAO.validateExperimentAccess(xpURI, currentUser);

        return sparql.getByURI(SPARQLDeserializers.nodeURI(xpURI), ScientificObjectModel.class, objectURI, currentUser.getLanguage());
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

    public void create(ScientificObjectClassModel model, UserModel currentUser) throws Exception {
        OntologyDAO dao = new OntologyDAO(sparql);
        dao.createClass(model, ScientificObjectClassModel.class);
    }
    
    public void getTypesTree(URI parentURI, UserModel currentUser) throws Exception {
        
        OntologyDAO dao = new OntologyDAO(sparql);

        SPARQLTreeListModel<ScientificObjectClassModel> classTree = dao.searchSubClasses(parentURI, ScientificObjectClassModel.class, currentUser, true, (soModel) -> {
            
        });

    }
}
