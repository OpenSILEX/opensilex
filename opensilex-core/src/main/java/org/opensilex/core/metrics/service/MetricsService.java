package org.opensilex.core.metrics.service;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Accumulators;
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
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataSearchFilter;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.metrics.dal.*;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static org.opensilex.core.data.dal.DataModel.VARIABLE_FIELD;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class MetricsService {

    private final MongoDBService mongodb;
    private final SPARQLService sparql;
    private final ExperimentSummaryDao experimentSummaryDao;
    private final SystemSummaryDao systemSummaryDao;

    private final DataDAO dataDAO;
    private final VariableDAO variableDAO;
    private final ExperimentDAO experimentDAO;

    public MetricsService(MongoDBService mongodb, SPARQLService sparql) {
        this.mongodb = mongodb;
        this.sparql = sparql;
        dataDAO = new DataDAO(mongodb, sparql, null);
        variableDAO = new VariableDAO(sparql, mongodb, null);
        experimentDAO = new ExperimentDAO(sparql);
        experimentSummaryDao = new ExperimentSummaryDao(mongodb);
        systemSummaryDao = new SystemSummaryDao(mongodb);
    }

    private HashMap<URI, String> getUsedVariablesFromDataDAO(String lang) throws Exception {

        Set<URI> variableURIs = dataDAO.distinct(VARIABLE_FIELD, URI.class, new DataSearchFilter(), null);
        List<VariableModel> variableModels = variableDAO.getList(variableURIs, lang);

        HashMap<URI, String> variables = new HashMap<>();
        for (VariableModel usedVariable : variableModels) {
            variables.put(usedVariable.getUri(), usedVariable.getName());
        }

        return variables;
    }

    private void updateSummaryEntries(CountListItemModel countItemList, URI classURI, String lang) throws SPARQLException {

        for (CountItemModel countItem : countItemList.getItems()) {
            URI itemTypeURI = countItem.getUri();
            ClassModel typeModel = SPARQLModule.getOntologyStoreInstance().getClassModel(itemTypeURI, classURI, lang);
            countItem.setName(typeModel.getName());
        }
    }

    public ListWithPagination<ExperimentSummaryModel> searchExperimentSummaries(ExperimentSummarySearchFilter filter) throws Exception {

        List<OrderBy> orderByList = Collections.singletonList(new OrderBy(ExperimentSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        filter.setOrderByList(orderByList);

        ListWithPagination<ExperimentSummaryModel> results = experimentSummaryDao.search(filter);

        URI osTypeURI = URI.create(Oeso.ScientificObject.getURI());
        URI germplasmTypeURI = URI.create(Oeso.Germplasm.getURI());
        HashMap<URI, String> variables = getUsedVariablesFromDataDAO(filter.getLang());

        for (ExperimentSummaryModel model : results.getList()) {
            updateSummaryEntries(model.getScientificObjectsByType(), osTypeURI, filter.getLang());
            updateSummaryEntries(model.getGermplasmByType(), germplasmTypeURI, filter.getLang());

            for (CountItemModel item : model.getDataByVariables().getItems()) {
                item.setName(variables.get(item.getUri()));
            }
        }

        return results;
    }

    public ListWithPagination<SystemSummaryModel> searchSystemSummaries(SystemSummarySearchFilter filter) throws Exception {

        List<OrderBy> orderByList = Collections.singletonList(new OrderBy(ExperimentSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        filter.setOrderByList(orderByList);

        ListWithPagination<SystemSummaryModel> results = systemSummaryDao.search(filter);

        URI deviceTypeURI = URI.create(Oeso.Device.getURI());
        URI osTypeURI = URI.create(Oeso.ScientificObject.getURI());
        URI germplasmTypeURI = URI.create(Oeso.Germplasm.getURI());
        HashMap<URI, String> variables = getUsedVariablesFromDataDAO(filter.getLang());

        for (SystemSummaryModel model : results.getList()) {
            updateSummaryEntries(model.getScientificObjectsByType(), osTypeURI, filter.getLang());
            updateSummaryEntries(model.getGermplasmByType(), germplasmTypeURI, filter.getLang());
            updateSummaryEntries(model.getDeviceByType(), deviceTypeURI, filter.getLang());

            for (CountItemModel item : model.getDataByVariables().getItems()) {
                item.setName(variables.get(item.getUri()));
            }
        }

        return results;
    }

    public void createExperimentSummary(ClientSession session) throws Exception {
        AccountModel currentUser = AccountModel.getSystemUser();

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

//            mongodb.create(model, GlobalSummaryModel.class, METRICS_COLLECTION, "experiment");
        }
    }


    private CountListItemModel getCountByTypeAndContext(URI contextURI, AccountModel currentUser, Resource classResource, boolean isExperimentContext) throws Exception {
        SelectBuilder select = new SelectBuilder();

        Var varUri = makeVar("uri");
        Var varType = makeVar("type");
        Var varG = makeVar("g");
        Var varCount = makeVar("uri_count");
        Var varLabel = makeVar("label");

        if (contextURI != null) {
            Node context = SPARQLDeserializers.nodeURI(contextURI);
            select.addGraph(context, varUri, RDF.type, varType);
        } else if (Boolean.TRUE.equals(!currentUser.isAdmin()) && isExperimentContext) {
            Set<URI> graphFilterURIs = experimentDAO.getUserExperiments(currentUser);

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
                countItem.setName(label);
                types.addItem(countItem);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });
        return types;
    }

    private CountListItemModel getDataCountByVariables(URI experimentURI, AccountModel currentUser) throws Exception {
        List<URI> experiments = null;
        if (experimentURI != null) {
            experiments = Collections.singletonList(experimentURI);
        }

        DataSearchFilter dataSearchFilter = new DataSearchFilter().setExperiments(experiments);
        dataSearchFilter.setAccountURI(currentUser.getUri());

        Set<URI> variableURIs = dataDAO.distinct(VARIABLE_FIELD,URI.class, dataSearchFilter, null);

        // Make an aggregation on DATA_COLLECTION_NAME
        Document filter = new Document();
        Document inFilter = new Document();
        inFilter.put("$in", new ArrayList<>(variableURIs));
        filter.put("variable", inFilter);
        // Add experiment filter
        dataDAO.appendExperimentUserAccessFilter(filter, currentUser, experiments);
        // 1.match - Filter data on these variables
        // 2.group - Group variables with number of data
        Set<Document> variablesWithDataCount = mongodb.aggregate(
                DataDAO.DATA_COLLECTION_NAME,
                Arrays.asList(match(filter),
                        group("$variable", Accumulators.sum("count", 1))),
                Document.class);
        CountListItemModel variablesByCount = new CountListItemModel();
        variablesByCount.setType(new URI(Oeso.Variable.getURI()));
        variablesByCount.setName(Oeso.Variable.getLocalName());
        // Return a list of unique arrays keys

        URI variableTypeURI = URI.create(Oeso.Variable.getURI());

        for (Document variableWithDataCount : variablesWithDataCount) {
            URI variableURI = URI.create((String) variableWithDataCount.get("_id"));
            CountItemModel countItem = new CountItemModel();
            countItem.setUri(variableURI);
            countItem.setCount((Integer) variableWithDataCount.get("count"));
            countItem.setType(variableTypeURI);
            variablesByCount.addItem(countItem);
        }

        return variablesByCount;
    }

}
