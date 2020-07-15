/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import org.opensilex.core.ontology.dal.CSVValidationModel;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.dal.BuiltInDatatypes;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OntologyDAO;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
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

        Map<String, OwlRestrictionModel> restrictionsByHeader = new HashMap<>();

        OntologyDAO ontologyDAO = new OntologyDAO(sparql);
        ClassModel model = ontologyDAO.getClassModel(soType, currentUser.getLanguage());

        model.getOrderedRestrictions().forEach(restriction -> {
            if (!restriction.isList()) {
                URI propertyURI = restriction.getOnProperty();

                if (model.isDatatypePropertyRestriction(propertyURI)) {
                    String header = model.getDatatypeProperty(propertyURI).getName();
                    restrictionsByHeader.put(header, restriction);
                } else if (model.isObjectPropertyRestriction(propertyURI)) {
                    String header = model.getObjectProperty(propertyURI).getName();
                    restrictionsByHeader.put(header, restriction);
                }
            }
        });

        Map<Integer, OwlRestrictionModel> restrictionsByIndex = new HashMap<>();
        Map<Integer, String> headerByIndex = new HashMap<>();

        CSVValidationModel csvErrors = new CSVValidationModel();

        try (CSVReader csvReader = new CSVReader(new FileReader(file));) {
            String[] headers = csvReader.readNext();

            if (headers != null) {

                for (int i = 0; i < headers.length; i++) {
                    String header = headers[i];
                    if (restrictionsByHeader.containsKey(header)) {
                        restrictionsByIndex.put(i, restrictionsByHeader.get(header));
                        headerByIndex.put(i, header);
                        restrictionsByHeader.remove(header);
                    }
                }

                if (!restrictionsByHeader.isEmpty()) {
                    csvErrors.addMissingHeaders(restrictionsByHeader.keySet());
                    return csvErrors;
                }

                Map<URI, Map<URI, Boolean>> checkedClassObjectURIs = new HashMap<>();

                int rowIndex = 1;
                String[] values = null;
                while ((values = csvReader.readNext()) != null) {
                    for (int colIndex = 0; colIndex < values.length; colIndex++) {
                        if (restrictionsByIndex.containsKey(colIndex)) {
                            String value = values[colIndex].trim();
                            OwlRestrictionModel restriction = restrictionsByIndex.get(colIndex);
                            URI propertyURI = restriction.getOnProperty();

                            if (restriction.isRequired() && (value == null || value.isEmpty())) {
                                csvErrors.addMissingRequiredValue(rowIndex, colIndex, headerByIndex.get(colIndex));
                            } else if (model.isDatatypePropertyRestriction(propertyURI)) {
                                BuiltInDatatypes dataType = BuiltInDatatypes.getBuiltInDatatype(restriction.getSubjectURI());
                                if (!dataType.validate(value)) {
                                    csvErrors.addInvalidDatatypeError(rowIndex, colIndex, headerByIndex.get(colIndex), dataType, value);
                                }
                            } else if (model.isObjectPropertyRestriction(propertyURI)) {
                                try {
                                    URI objectURI = new URI(value);
                                    if (objectURI.isAbsolute()) {
                                        URI classURI = restriction.getSubjectURI();
                                        boolean doesClassObjectUriExist;
                                        if (checkedClassObjectURIs.containsKey(classURI) && checkedClassObjectURIs.get(classURI).containsKey(objectURI)) {
                                            doesClassObjectUriExist = checkedClassObjectURIs.get(classURI).get(objectURI);
                                        } else {
                                            doesClassObjectUriExist = sparql.uriExists(classURI, objectURI);
                                            if (!checkedClassObjectURIs.containsKey(classURI)) {
                                                checkedClassObjectURIs.put(classURI, new HashMap<>());
                                            }
                                            checkedClassObjectURIs.get(classURI).put(objectURI, doesClassObjectUriExist);
                                        }

                                        if (!doesClassObjectUriExist) {
                                            csvErrors.addURINotFoundError(rowIndex, colIndex, headerByIndex.get(colIndex), classURI, objectURI);
                                        }
                                    } else {
                                        csvErrors.addInvalidURIError(rowIndex, colIndex, headerByIndex.get(colIndex), value);
                                    }
                                } catch (URISyntaxException ex) {
                                    csvErrors.addInvalidURIError(rowIndex, colIndex, headerByIndex.get(colIndex), value);
                                }
                            }
                        }
                    }

                    rowIndex++;
                }
            }
        }

        return csvErrors;
    }
}
