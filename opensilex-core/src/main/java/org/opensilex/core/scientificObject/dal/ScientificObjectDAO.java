/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import org.opensilex.core.ontology.dal.CSVValidationModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLPartialTreeListModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.service.SPARQLService;

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

    public void importCSV(URI xpURI, URI soType, File file, UserModel user) throws Exception {
        Reader in = new InputStreamReader(new FileInputStream(file));
        CSVParser rows = CSVFormat.DEFAULT.parse(in);

        Map<String, OwlRestrictionModel> headers = getCSVHeaderRestrictions(soType, user.getLanguage());

        for (CSVRecord row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                String value = row.get(i);
                ScientificObjectModel so = new ScientificObjectModel();
                so.setType(soType);
                switch (i) {
                    case 0:
                        so.setUri(new URI(value));
                        break;
                    case 1:
                        so.setName(value);
                        break;
                    case 2:
                        ScientificObjectModel parent = new ScientificObjectModel();
                        parent.setUri(new URI(value));
                        so.setParent(parent);
                        break;
                    default:

                        break;
                }

            }

        }
    }

    public Map<String, OwlRestrictionModel> getCSVHeaderRestrictions(URI soType, String lang) throws Exception {
        Map<String, OwlRestrictionModel> headersMap = new HashMap<>();

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, lang);

        model.getOrderedRestrictions().forEach(restriction -> {
            if (!restriction.isList()) {
                URI propertyURI = restriction.getOnProperty();

                if (model.isDatatypePropertyRestriction(propertyURI)) {
                    String header = model.getDatatypeProperty(propertyURI).getName();
                    headersMap.put(header, restriction);
                } else if (model.isObjectPropertyRestriction(propertyURI)) {
                    String header = model.getObjectProperty(propertyURI).getName();
                    headersMap.put(header, restriction);
                }
            }
        });

        return headersMap;
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

        return ontologyDAO.validateCSV(soType, file, currentUser, customValidators);
    }
}
