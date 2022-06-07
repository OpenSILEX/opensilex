//******************************************************************************
//                          MetricsDAO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.dal;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

/**
 * @author Arnaud CHARLEROY
 */
public class MetricsDAO {

    protected final SPARQLService sparql;
    protected final MongoDBService nosql;

    public static final String METRICS_COLLECTION = "metrics";

    public MetricsDAO(SPARQLService sparql, MongoDBService nosql) {
        this.sparql = sparql;
        this.nosql = nosql;
    }

    public void createIndexes() {
        IndexOptions unicityOptions = new IndexOptions().unique(true);

        MongoCollection<GlobalSummaryModel> metricsCollection = nosql.getDatabase()
                .getCollection(METRICS_COLLECTION, GlobalSummaryModel.class);
        metricsCollection.createIndex(Indexes.ascending(MongoModel.URI_FIELD), unicityOptions);
        metricsCollection.createIndex(Indexes.descending(GlobalSummaryModel.CREATION_DATE_FIELD));
    }

    public ListWithPagination<ExperimentSummaryModel> getExperimentSummaries(List<URI> experimentURIs, Instant startInstant, Instant endInstant, int page, int pageSize) throws NoSQLInvalidURIException {
        List<OrderBy> orderByList = Arrays.asList(new OrderBy(ExperimentSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        Document searchFilter = searchFilter(ExperimentSummaryModel.SUMMARY_TYPE, experimentURIs, startInstant, endInstant);
        ListWithPagination<ExperimentSummaryModel> searchWithPagination = nosql.searchWithPagination(ExperimentSummaryModel.class, METRICS_COLLECTION, searchFilter, orderByList, page, pageSize);

        return searchWithPagination;

    }

    public ListWithPagination<SystemSummaryModel> getSystemSummary(Instant startInstant, Instant endInstant, int page, int pageSize) throws NoSQLInvalidURIException {
        List<OrderBy> orderByList = Arrays.asList(new OrderBy(SystemSummaryModel.CREATION_DATE_FIELD, Order.DESCENDING));
        Document searchFilter = searchFilter(SystemSummaryModel.SUMMARY_TYPE, null, startInstant, endInstant);
        ListWithPagination<SystemSummaryModel> searchWithPagination = nosql.searchWithPagination(SystemSummaryModel.class, METRICS_COLLECTION, searchFilter, orderByList, page, pageSize);

        return searchWithPagination;

    }

    public Document searchFilter(String summaryType, List<URI> experiments, Instant startDate, Instant endDate) {

        Document filter = new Document();
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

        return filter;
    }

    public void createExperimentSummary() throws Exception {

        createIndexes();

        UserModel currentUser = UserModel.getSystemUser();

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

        UserModel currentUser = UserModel.getSystemUser();

        SystemSummaryModel model = new SystemSummaryModel();
        model.setCreationDate(Instant.now());
        CountListItemModel scientificObjectTypesByCount = getCountByTypeAndContext(null, currentUser, Oeso.ScientificObject, false);
        CountListItemModel deviceTypesByCount = getCountByTypeAndContext(null, currentUser, Oeso.Device, false);
        CountListItemModel variablesByCount = getDataCountByVariables(null, currentUser);
        CountListItemModel germplasmByCount = getCountByTypeAndContext(null, currentUser, Oeso.Germplasm, false);

        model.setScientificObjectsByType(scientificObjectTypesByCount);
        model.setDeviceByType(deviceTypesByCount);
        model.setDataByVariables(variablesByCount);
        model.setGermplasmByType(germplasmByCount);

        SPARQLConfig sparqlConfig = sparql.getOpenSilex().getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        model.setBaseSystemAlias(sparqlConfig.baseURIAlias());
        nosql.create(model, SystemSummaryModel.class, METRICS_COLLECTION, "system");

    }

    private CountListItemModel getDataCountByVariables(URI experimentURI, UserModel currentUser) throws Exception {
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
                Arrays.asList(match(filter), group("$variable", Accumulators.sum("count", 1))
                ));
        CountListItemModel variablesByCount = new CountListItemModel();
        variablesByCount.setType(new URI(Oeso.Variable.getURI()));
        variablesByCount.setName(Oeso.Variable.getLocalName());
        // Return a list of unique arrays keys
        for (Document variableWithDataCount : variablesWithDataCount) {
            VariableModel variable = tmpVariable.get(new URI((String) variableWithDataCount.get("_id")));
            CountItemModel countItem = new CountItemModel();
            countItem.setName(variable.getName());
            countItem.setUri(variable.getUri());
            countItem.setCount((Integer) variableWithDataCount.get("count"));
            countItem.setType(new URI(Oeso.Variable.getURI()));
            variablesByCount.addItem(countItem);
        }

        return variablesByCount;
    }

    private CountListItemModel getCountByTypeAndContext(URI contextURI, UserModel currentUser, Resource classResource, Boolean isExperimentContext) throws Exception {
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
                countItem.setName(label);
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
