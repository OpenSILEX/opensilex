package org.opensilex.core.event.dal.move;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * This class deals with all of MoveModels fields, other than it's noSqlModel
 */
public class MoveEventDAO extends EventDAO<MoveModel, MoveSearchFilter> {

    public static final Var fromNameVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectDefaultNameVarName(MoveModel.FROM_FIELD));
    public static final Var toNameVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectDefaultNameVarName(MoveModel.TO_FIELD));
    private static final Var lastEndTimeStampVar = SPARQLQueryHelper.makeVar("last_end_ts");
    private static final Triple moveToTriple = Triple.create(uriVar, Oeev.to.asNode(), toVar);
    private static final Triple moveTypeTriple = Triple.create(uriVar, RDF.type.asNode(), Oeev.Move.asNode());
    private static final TriplePath lastEndTimeStampMatchingTriple = new TriplePath(Triple.create(endInstantVar, Time.inXSDDateTimeStamp.asNode(), lastEndTimeStampVar));

    protected final static Logger LOGGER = LoggerFactory.getLogger(GeospatialDAO.class);

    public MoveEventDAO(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb, MoveModel.class);
    }

    //#region PUBLIC METHODS

    /**
     * @param models to be inserted into sparql graph
     * @return models
     * @throws Exception
     */
    @Override
    public List<MoveModel> create(List<MoveModel> models) throws Exception {

        sparql.createWithoutTransaction(graph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false, true);
        return models;
    }

    /**
     * @param eventUri
     * @param user
     * @return
     * @throws Exception
     */
    @Override
    public MoveModel get(URI eventUri, AccountModel user) throws Exception {
        return sparql.getByURI(MoveModel.class, eventUri, user.getLanguage());
    }

    @Override
    public List<MoveModel> getList(List<URI> uris, AccountModel user) throws Exception {
        return sparql.getListByURIs(MoveModel.class, uris, user.getLanguage());
    }

    @Override
    protected MoveModel fromResult(SPARQLResult result, String lang, MoveModel model) throws Exception {

        super.fromResult(result, lang, model);

        String fromStr = result.getStringValue(MoveModel.FROM_FIELD);
        if (!StringUtils.isEmpty(fromStr)) {
            FacilityModel from = new FacilityModel();
            from.setUri(new URI(fromStr));
            from.setName(result.getStringValue(fromNameVar.getVarName()));
            model.setFrom(from);
        }

        String toStr = result.getStringValue(MoveModel.TO_FIELD);
        if (!StringUtils.isEmpty(toStr)) {
            FacilityModel to = new FacilityModel();
            to.setUri(new URI(toStr));
            to.setName(result.getStringValue(toNameVar.getVarName()));
            model.setTo(to);
        }

        return model;
    }

    @Override
    public MoveModel update(MoveModel model) throws Exception {
        sparql.update(model);
        return model;
    }

    @Override
    public void delete(URI uri) throws Exception {
        sparql.delete(MoveModel.class, uri);
    }


    public Map<URI, URI> getLastLocations(Stream<URI> targets, int size) throws Exception {

        // use the MAX aggregator in order to compute the last (the highest considering a lexicographic order) end timestamp
        Aggregator maxEndTsAggregator = AggregatorFactory.createMax(false, SPARQLQueryHelper.getExprFactory().asExpr(endInstantTimeStampVar));

        // select for each target, the end timestamp of the last move event concerning the target
        // use inner select in order to have one timestamp per target
        SelectBuilder innerSelect = new SelectBuilder()
                .addVar(targetVar)
                .addVar(maxEndTsAggregator.asSparqlExpr(null), lastEndTimeStampVar)
                .addGraph(graph, new WhereBuilder()
                        .addWhere(targetTriple)
                        .addWhere(moveTypeTriple)
                        .addWhere(moveToTriple)
                        .addWhere(endTriple)
                        .addWhere(endInstantTimeStampTriple)
                ).addGroupBy(targetVar);

        // append VALUES on ?targets
        SPARQLQueryHelper.addWhereUriValues(innerSelect, targetVar.getVarName(), targets, size);

        SelectBuilder outerSelect = new SelectBuilder()
                .addVar(targetVar)
                .addVar(toVar)
                .addGraph(graph, new WhereBuilder()
                        .addWhere(lastEndTimeStampMatchingTriple)  // match with inner ?last_end_ts
                        .addWhere(targetTriple) // match with inner ?targets
                        .addWhere(endTriple)
                        .addWhere(moveToTriple)
                        .addSubQuery(innerSelect)
                );

        Map<URI, URI> targetLastLocations = new HashMap<>();

        sparql.executeSelectQueryAsStream(outerSelect).forEach(
            result -> {
                String lastLocation = result.getStringValue(MoveModel.TO_FIELD);
                if (lastLocation != null) {
                    String target = result.getStringValue(MoveModel.TARGETS_FIELD);
                    if (target != null) {
                        targetLastLocations.put(URIDeserializer.formatURI(target), URIDeserializer.formatURI(lastLocation));
                    }
                }
        });
        return targetLastLocations;

    }

    //#endregion

    //#region PROTECTED/PRIVATE METHODS

    @Override
    protected void appendAllFilters(SelectBuilder select, MoveSearchFilter filter) throws Exception {
        super.appendAllFilters(select, filter);
        ElementGroup rootElementGroup = select.getWhereHandler().getClause();
        ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, graph);
        appendTimeAfterFilter(eventGraphGroupElem, filter.getAfterEnd());
        if (CollectionUtils.isEmpty(filter.getOrderByList())) {
            select.addOrderBy(endInstantTimeStampVar, Order.DESCENDING);
        }
    }

    @Override
    protected Map<String, WhereHandler> getCustomHandlerForFields(MoveSearchFilter filter) throws Exception {
        return null;
    }

    /**
     *
     * @param eventGraphGroupElem
     * @param dateTime , if null then function does nothing
     * @throws Exception
     */
    private void appendTimeAfterFilter(ElementGroup eventGraphGroupElem, OffsetDateTime dateTime) throws Exception {
        if (dateTime != null) {
            Node dateTimeNode = SPARQLDeserializers.getForClass(OffsetDateTime.class).getNode(dateTime);
            Expr dateTimeExpr = SPARQLQueryHelper.getExprFactory().le(endInstantTimeStampVar, dateTimeNode);
            eventGraphGroupElem.addElementFilter(new ElementFilter(dateTimeExpr));
        }
    }

    //#endregion

}
