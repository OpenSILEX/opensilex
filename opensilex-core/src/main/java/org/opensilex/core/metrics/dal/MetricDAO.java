//******************************************************************************
//                          MetricsDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.bson.Document;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLInvalidURIException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Arnaud CHARLEROY
 */
public class MetricDAO {
    protected final SPARQLService sparql;
    protected final MongoDBService nosql;

    private AccountModel user;

    private final static Logger Logger = LoggerFactory.getLogger(MetricDAO.class);
    public static final String METRICS_COLLECTION = "metrics";

    public MetricDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
    }

    public MetricDAO(SPARQLService sparql, MongoDBService nosql, AccountModel user) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.user = user;
    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection<GlobalSummaryModel> metricsCollection = nosql.getDatabase()
                .getCollection(METRICS_COLLECTION, GlobalSummaryModel.class);
        metricsCollection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
        metricsCollection.createIndex(Indexes.descending(GlobalSummaryModel.CREATION_DATE_FIELD));
    }

    public ListWithPagination<ExperimentSummaryModel> getExperimentSummaries(List<URI> experimentURIs, Instant startInstant, Instant endInstant, int page, int pageSize, String currentLanguage) throws URISyntaxException, Exception {
        List<OrderBy> orderByList = Arrays.asList(new OrderBy(ExperimentSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        Document searchFilter = searchFilter(ExperimentSummaryModel.SUMMARY_TYPE, experimentURIs, startInstant, endInstant);

        ListWithPagination<ExperimentSummaryModel> searchWithPagination = nosql.searchWithPagination(ExperimentSummaryModel.class, METRICS_COLLECTION, searchFilter, orderByList, page, pageSize);

        HashMap<URI, String> variables = getUsedVariablesFromDataDAO();

        // for each entry in metrics
        searchWithPagination.getList().forEach(metric -> {
            // get items uris
            List<URI> scientificObjectsURIs = metric.getScientificObjectsByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> germplasmsURIs = metric.getGermplasmByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            try {
                //scientific objects
                HashMap<URI, String> scientificObjects = new HashMap<>();
                for (URI scientificObjectURI : scientificObjectsURIs) {
                    ClassModel scientificObjectType = SPARQLModule.getOntologyStoreInstance().getClassModel(scientificObjectURI, URI.create(Oeso.ScientificObject.getURI()), currentLanguage);
                    scientificObjects.put(scientificObjectURI, scientificObjectType.getName());
                }
                for (CountItemModel item : metric.getScientificObjectsByType().getItems()) {
                    item.setName(scientificObjects.get(item.getUri()));
                }

                //germplasms
                HashMap<URI, String> germplasms = new HashMap<>();
                for (URI germplasmURI : germplasmsURIs) {
                    ClassModel germplasmType = SPARQLModule.getOntologyStoreInstance().getClassModel(germplasmURI, URI.create(Oeso.Germplasm.getURI()), currentLanguage);
                    germplasms.put(germplasmURI, germplasmType.getName());
                }
                for (CountItemModel item : metric.getGermplasmByType().getItems()) {
                    item.setName(germplasms.get(item.getUri()));
                }

                //variables
                for (CountItemModel item : metric.getDataByVariables().getItems()) {
                    URI itemUri = new URI(SPARQLDeserializers.getExpandedURI(item.getUri()));
                    String itemName = variables.get(itemUri);
                    item.setName(itemName);
                }
                
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return searchWithPagination;

    }

    public ListWithPagination<SystemSummaryModel> getSystemSummaryWithPagination(Instant startInstant, Instant endInstant, int page, int pageSize, String currentLanguage) throws Exception {
        List<OrderBy> orderByList = Arrays.asList(new OrderBy(SystemSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        Document searchFilter = searchFilter(SystemSummaryModel.SUMMARY_TYPE, null, startInstant, endInstant);

        ListWithPagination<SystemSummaryModel> searchWithPagination = nosql.searchWithPagination(SystemSummaryModel.class, METRICS_COLLECTION, searchFilter, orderByList, page, pageSize);

        HashMap<URI, String> variables = getUsedVariablesFromDataDAO();

        // for each entry in metrics
        searchWithPagination.getList().forEach(summary -> {
            // get items uris
            List<URI> deviceURIs = summary.getDeviceByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> scientificObjectsURIs = summary.getScientificObjectsByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> germplasmsURIs = summary.getGermplasmByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> experimentURIs = summary.getExperimentByType() == null ? Collections.emptyList() :
                    summary.getExperimentByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            try {
                // devices: map uri to its name
                HashMap<URI, String> devices = new HashMap<>();
                for (URI deviceURI : deviceURIs) {
                    ClassModel deviceType = SPARQLModule.getOntologyStoreInstance().getClassModel(deviceURI,URI.create(Oeso.Device.getURI()),currentLanguage);
                    devices.put(deviceURI, deviceType.getName());
                }
                // add name to metric entry
                for (CountItemModel item : summary.getDeviceByType().getItems()) {
                    item.setName(devices.get(item.getUri()));
                }

                //scientific objects
                HashMap<URI, String> scientificObjects = new HashMap<>();
                for (URI scientificObjectURI : scientificObjectsURIs) {
                    ClassModel scientificObjectType = SPARQLModule.getOntologyStoreInstance().getClassModel(scientificObjectURI,URI.create(Oeso.ScientificObject.getURI()),currentLanguage);
                    scientificObjects.put(scientificObjectURI, scientificObjectType.getName());
                }
                for (CountItemModel item : summary.getScientificObjectsByType().getItems()) {
                    item.setName(scientificObjects.get(item.getUri()));
                }

                //germplasms
                HashMap<URI, String> germplasms = new HashMap<>();
                for (URI germplasmURI : germplasmsURIs) {
                    ClassModel germplasmType = SPARQLModule.getOntologyStoreInstance().getClassModel(germplasmURI,URI.create(Oeso.Germplasm.getURI()),currentLanguage);
                    germplasms.put(germplasmURI, germplasmType.getName());
                }
                for (CountItemModel item : summary.getGermplasmByType().getItems()) {
                    item.setName(germplasms.get(item.getUri()));
                }

                //variables
                for (CountItemModel item : summary.getDataByVariables().getItems()) {
                    URI itemUri = new URI(SPARQLDeserializers.getExpandedURI(item.getUri()));
                    String itemName = variables.get(itemUri);
                    item.setName(itemName);
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return searchWithPagination;

    }

    public List<SystemSummaryModel> getSystemSummaryFirstAndLastByPeriod(Instant startInstant, Instant endInstant, String currentLanguage) throws Exception {
        List<SystemSummaryModel>  searchFirstLast = null;

        //latest only
        List<OrderBy> orderDescendByList = Arrays.asList(new OrderBy(SystemSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        Document latestSearchFilter = searchFilter(SystemSummaryModel.SUMMARY_TYPE, null, null, endInstant);
        SystemSummaryModel latestSmodel = setSystemSummaryModelForSearchResult(latestSearchFilter, orderDescendByList, currentLanguage, null, endInstant);

        if (latestSmodel != null) {
            //oldest only
            List<OrderBy> orderAscendByList = Arrays.asList(new OrderBy(SystemSummaryModel.CREATION_DATE_FIELD, Order.ASCENDING));
            Document oldestSearchFilter = searchFilter(SystemSummaryModel.SUMMARY_TYPE, null, startInstant, null);
            SystemSummaryModel oldestSmodel = setSystemSummaryModelForSearchResult(oldestSearchFilter, orderAscendByList, currentLanguage, startInstant, null);

            searchFirstLast = (oldestSmodel != null) ? Arrays.asList(latestSmodel, oldestSmodel) : Arrays.asList(latestSmodel);
        }

        return searchFirstLast;
    }

    private SystemSummaryModel setSystemSummaryModelForSearchResult(Document searchFilter, List<OrderBy> orderByList, String currentLanguage, Instant startInstant, Instant endInstant) throws Exception {
        ListWithPagination<SystemSummaryModel> searchWithPagination = nosql.searchWithPagination(SystemSummaryModel.class, METRICS_COLLECTION, searchFilter, orderByList, 0, 1);
        HashMap<URI, String> variables = getUsedVariablesFromDataDAO();

        SystemSummaryModel summary = (searchWithPagination.getTotal() == 0) ? null : searchWithPagination.getList().get(0);

        if(summary != null){
            // get items uris
            List<URI> deviceURIs = summary.getDeviceByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> scientificObjectsURIs = summary.getScientificObjectsByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> germplasmsURIs = summary.getGermplasmByType().getItems().stream()
                    .map(CountItemModel::getUri)
                    .collect(Collectors.toList());
            List<URI> experimentURIs = summary.getExperimentByType() == null ? Collections.emptyList() :
                    summary.getExperimentByType().getItems().stream()
                            .map(CountItemModel::getUri)
                            .collect(Collectors.toList());
            try {
                // devices: map uri to its name
                HashMap<URI, String> devices = new HashMap<>();
                for (URI deviceURI : deviceURIs) {
                    try {
                        ClassModel deviceType = SPARQLModule.getOntologyStoreInstance().getClassModel(deviceURI, URI.create(Oeso.Device.getURI()), currentLanguage);
                        devices.put(deviceURI, deviceType.getName());
                    } catch (SPARQLInvalidURIException e) {
                        Logger.warn("Device URI not found", e);
                        continue;
                    }
                }
                // add name to metric entry
                for (CountItemModel item : summary.getDeviceByType().getItems()) {
                    item.setName(devices.get(item.getUri()));
                }

                //scientific objects
                HashMap<URI, String> scientificObjects = new HashMap<>();
                for (URI scientificObjectURI : scientificObjectsURIs) {
                    try {
                        ClassModel scientificObjectType = SPARQLModule.getOntologyStoreInstance().getClassModel(scientificObjectURI, URI.create(Oeso.ScientificObject.getURI()), currentLanguage);
                        scientificObjects.put(scientificObjectURI, scientificObjectType.getName());
                    } catch (SPARQLInvalidURIException e) {
                        Logger.warn("Scientific Object URI not found", e);
                        continue;
                    }
                }
                for (CountItemModel item : summary.getScientificObjectsByType().getItems()) {
                    item.setName(scientificObjects.get(item.getUri()));
                }

                //experiments
                HashMap<URI, String> experiments = new HashMap<>();
                for (URI experimentURI : experimentURIs) {
                    try {
                        ClassModel experimentType = SPARQLModule.getOntologyStoreInstance().getClassModel(experimentURI, URI.create(Oeso.Experiment.getURI()), currentLanguage);
                        experiments.put(experimentURI, experimentType.getName());
                    } catch (SPARQLInvalidURIException e) {
                        Logger.warn("Experiment URI not found", e);
                        continue;
                    }
                }
                for (CountItemModel item : summary.getExperimentByType().getItems()) {
                    item.setName(experiments.get(item.getUri()));
                }

                //germplasms
                HashMap<URI, String> germplasms = new HashMap<>();
                for (URI germplasmURI : germplasmsURIs) {
                    try {
                        ClassModel germplasmType = SPARQLModule.getOntologyStoreInstance().getClassModel(germplasmURI, URI.create(Oeso.Germplasm.getURI()), currentLanguage);
                        germplasms.put(germplasmURI, germplasmType.getName());
                    } catch (SPARQLInvalidURIException e) {
                        Logger.warn("Germplasm URI not found", e);
                        continue;
                    }
                }
                for (CountItemModel item : summary.getGermplasmByType().getItems()) {
                    item.setName(germplasms.get(item.getUri()));
                }

                //variables
                for (CountItemModel item : summary.getDataByVariables().getItems()) {
                    URI itemUri = new URI(SPARQLDeserializers.getExpandedURI(item.getUri()));
                    String itemName = variables.get(itemUri);
                    item.setName(itemName);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return summary;
    }

    private HashMap<URI, String> getUsedVariablesFromDataDAO() throws Exception {
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);
        List<VariableModel> usedVariables = dataDAO.getUsedVariables(user, null, null, null, null);

        HashMap<URI, String> variables = new HashMap<>();

        for (VariableModel usedVariable : usedVariables) {
            variables.put(usedVariable.getUri(), usedVariable.getName());
        }

        return variables;
    }

    public Document searchFilter(String summaryType, List<URI> experiments, Instant startDate, Instant endDate) {
        Document filter = new Document();//MongoDB class Document
        filter.append(GlobalSummaryModel.SUMMARY_TYPE_FIELD, summaryType);
        if (experiments != null && !experiments.isEmpty()) {
            Document inFilter = new Document();
            inFilter.put("$in", experiments);
            filter.put(ExperimentSummaryModel.EXPERIMENT_FIELD, inFilter);
        }

        if (startDate != null || endDate != null) {
            Document dateFilter = new Document();
            if (startDate != null) {
                dateFilter.put("$gte", startDate);
            }

            if (endDate != null) {
                dateFilter.put("$lt", endDate);
            }
            filter.put(SystemSummaryModel.CREATION_DATE_FIELD, dateFilter);
        }
        Logger.debug("MetricsDAO:searchFilter() set to: " + filter.toString());
        return filter;
    }

    public void createExperimentSummary() throws Exception {

        createIndexes();

        AccountModel currentUser = AccountModel.getSystemUser();

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);

        Set<URI> experiments = experimentDAO.getUserExperiments(currentUser);

        for (URI experimentURI : experiments) {
            ExperimentSummaryModel model = new ExperimentSummaryModel();
            model.setExperimentUri(experimentURI);
            model.setCreationDate(Instant.now());
            CountListItemModel scientificObjectTypesByCount = getCountByTypeAndContext(experimentURI, currentUser, Oeso.ScientificObject, false);
            CountListItemModel variables = getDataCountByVariables(experimentURI, currentUser);
            CountListItemModel germplasmByCount = getCountByTypeAndContext(experimentURI, currentUser, Oeso.Germplasm, false);

            model.setScientificObjectsByType(scientificObjectTypesByCount);
            model.setDataByVariables(variables);
            model.setGermplasmByType(germplasmByCount);

            nosql.create(model, GlobalSummaryModel.class, METRICS_COLLECTION, "experiment");
        }

    }

    public void createSystemSummary() throws Exception {
        createIndexes();

        AccountModel currentUser = AccountModel.getSystemUser();

        SystemSummaryModel model = new SystemSummaryModel();
        model.setCreationDate(Instant.now());
        CountListItemModel scientificObjectTypesByCount = getCountByTypeAndContext(null, currentUser, Oeso.ScientificObject, false);
        CountListItemModel deviceTypesByCount = getCountByTypeAndContext(null, currentUser, Oeso.Device, false);
        CountListItemModel variablesByCount = getDataCountByVariables(null, currentUser);
        CountListItemModel germplasmByCount = getCountByTypeAndContext(null, currentUser, Oeso.Germplasm, false);
        // TODO : Refactor experiment , don't use
        CountListItemModel experimentsByCount = new CountListItemModel();
        experimentsByCount.setName("Experiment");
        experimentsByCount.setType(new URI(Oeso.Experiment.getURI()));
        ExperimentDAO expDao = new ExperimentDAO(sparql,nosql);
        experimentsByCount.setCalculatedTotalCount(expDao.count());

        model.setScientificObjectsByType(scientificObjectTypesByCount);
        model.setDeviceByType(deviceTypesByCount);
        model.setDataByVariables(variablesByCount);
        model.setGermplasmByType(germplasmByCount);
        model.setExperimentByType(experimentsByCount);

        SPARQLConfig sparqlConfig = sparql.getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        model.setBaseSystemAlias(sparqlConfig.baseURIAlias());
        nosql.create(model, SystemSummaryModel.class, METRICS_COLLECTION, "system");
    }

    private CountListItemModel getDataCountByVariables(URI experimentURI, AccountModel currentUser) throws Exception {
        List<URI> experiments = null;
        if (experimentURI != null) {
            experiments = Arrays.asList(experimentURI);
        }
        DataDAO dataDAO = new DataDAO(nosql, sparql, null);

        List<VariableModel> variables = dataDAO.getUsedVariables(currentUser, experiments, null, null, null);
        Map<URI, VariableModel> tmpVariable = new HashMap<>();
        for (VariableModel variable : variables) {
            tmpVariable.put(new URI(SPARQLDeserializers.getExpandedURI(variable.getUri())), variable);
        }

        // Make an aggregation on DATA_COLLECTION_NAME
        Document filter = new Document();
        Document inFilter = new Document();
        inFilter.put("$in", new ArrayList<>(tmpVariable.keySet()));
        filter.put("variable", inFilter);
        // Add experiment filter
        dataDAO.appendExperimentUserAccessFilter(filter, currentUser, experiments);
        // 1.match - Filter data on these variables
        // 2.group - Group variables with number of data  
        Set<Document> variablesWithDataCount = nosql.aggregate(
                DataDAO.DATA_COLLECTION_NAME,
                Arrays.asList(match(filter),
                    group("$variable", Accumulators.sum("count", 1))),
                    Document.class);
        CountListItemModel variablesByCount = new CountListItemModel();
        variablesByCount.setType(new URI(Oeso.Variable.getURI()));
        variablesByCount.setName(Oeso.Variable.getLocalName());
        // Return a list of unique arrays keys
        for (Document variableWithDataCount : variablesWithDataCount) {
            VariableModel variable = tmpVariable.get(new URI((String) variableWithDataCount.get("_id")));
            CountItemModel countItem = new CountItemModel();
            countItem.setUri(variable.getUri());
            countItem.setCount((Integer) variableWithDataCount.get("count"));
            countItem.setType(new URI(Oeso.Variable.getURI()));
            variablesByCount.addItem(countItem);
        }

        return variablesByCount;
    }

    private CountListItemModel getCountByTypeAndContext(URI contextURI, AccountModel currentUser, Resource classResource, Boolean isExperimentContext) throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var varUri = makeVar("uri");
        Var varType = makeVar("type");
        Var varG = makeVar("g");
        Var varCount = makeVar("uri_count");
        Var varLabel = makeVar("label");

        if (contextURI != null) {
            Node context = SPARQLDeserializers.nodeURI(contextURI);
            select.addGraph(context, varUri, RDF.type, varType);
        } else if (!currentUser.isAdmin() && isExperimentContext) {
            ExperimentDAO xpDO = new ExperimentDAO(sparql, nosql);
            Set<URI> graphFilterURIs = xpDO.getUserExperiments(currentUser);

            select.addGraph(varG, varUri, RDF.type, varType);
            select.addFilter(SPARQLQueryHelper.inURIFilter(varG, graphFilterURIs));
        } else {
            select.addWhere(varUri, RDF.type, varType);
        }

        select.addVar(varType);
        select.addVar("(count(distinct ?uri) as ?uri_count)", varCount);
        select.addVar(varLabel);

        select.addWhere(varType, Ontology.subClassStrict, classResource);
        select.addWhere(varType, RDFS.label, varLabel);
        select.addFilter(SPARQLQueryHelper.langFilter(varLabel, currentUser.getLanguage()));

        select.addGroupBy(varType);
        select.addGroupBy(varLabel);

        CountListItemModel types = new CountListItemModel();
        types.setType(new URI(classResource.getURI()));
        types.setName(classResource.getLocalName());
        sparql.executeSelectQuery(select, (row) -> {
            CountItemModel countItem = new CountItemModel();
            URI uri;
            try {
                uri = new URI(row.getStringValue(varType.getVarName()));
                countItem.setUri(uri);
                String label = row.getStringValue(varLabel.getVarName());
                Integer uriCount = Integer.valueOf(row.getStringValue(varCount.getVarName()));
                countItem.setCount(uriCount);
                countItem.setType(new URI(OWL2.Class.getURI()));
                types.addItem(countItem);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });
        return types;
    }

}
