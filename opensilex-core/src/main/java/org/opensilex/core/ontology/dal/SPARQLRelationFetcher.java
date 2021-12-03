package org.opensilex.core.ontology.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementOptional;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.opensilex.OpenSilex;
import org.opensilex.core.CoreModule;
import org.opensilex.core.ontology.dal.cache.OntologyCache;
import org.opensilex.core.ontology.dal.cache.OntologyCacheException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.PropertyModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Class used to optimize the fetching of multiple {@link SPARQLModelRelation} for a List of T, by retrieving
 * all those relations in one SPARQL query.
 *
 * This class build a SPARQL query which use the same WHERE clause as the initial SPARQL query.
 * Then the query is updated by adding a triple and a var corresponding to each data/object property which is defined into the ontology for
 * the T type.
 *
 * Then, results from the new SPARQL query are linked to the initial T list, in order to update element of T.
 *
 * @param <T> : a type of {@link SPARQLModelRelation}
 */
public class SPARQLRelationFetcher<T extends SPARQLResourceModel> {

    private final SPARQLService sparql;

    private final Node graph;
    private final URI graphUri;
    private final SelectBuilder initialSelect;
    private final List<T> results;

    private final Set<PropertyModel> typesProperties;
    private final Map<URI, List<URI>> propertiesByType;
    private final Map<URI, List<String>> propertiesByTypeVarNames;

    private final Pattern specialCharsPattern;

    public SPARQLRelationFetcher(SPARQLService sparql, Class<T> objectClass, Node graph, SelectBuilder initialSelect, List<T> results) throws URISyntaxException, SPARQLException, OntologyCacheException {

        this.sparql = sparql;
        SPARQLClassObjectMapper<T> mapper = sparql.getMapperIndex().getForClass(objectClass);
        OntologyCache ontologyCache = CoreModule.getOntologyCacheInstance();

        this.graph = graph;
        this.graphUri = graph != null ? new URI(graph.getURI()) : null;
        this.initialSelect = initialSelect;
        this.results = results;

        //  #TODO optimize k types extraction from n results ?
        //   most of the time, k is much smaller than n, iteration could be costly with lot of results
        Set<URI> types = results.stream().map(SPARQLResourceModel::getType).collect(Collectors.toSet());

        Set<URI> managedProperties = new HashSet<>();
        for (Property property : mapper.getClassAnalizer().getManagedProperties()) {
            managedProperties.add(URIDeserializer.formatURI(property.toString()));
        }

        this.typesProperties = new HashSet<>();
        this.propertiesByType = new HashMap<>();
        this.propertiesByTypeVarNames = new HashMap<>();

        specialCharsPattern = Pattern.compile("[^A-Za-z0-9]");

        for (URI type : types) {

            // get class from OntologyCache and compute stream of data/object property
            ClassModel classModel = ontologyCache.getClassModel(type, OpenSilex.DEFAULT_LANGUAGE);
            Stream<PropertyModel> propertyStream = Stream.concat(
                    classModel.getDatatypeProperties().values().stream(),
                    classModel.getObjectProperties().values().stream()
            );

            List<URI> propertiesUris = new ArrayList<>();
            List<String> propertiesNames = new ArrayList<>();

            // update properties set and type <-> properties index
            propertyStream.forEach(property -> {

                // use short uri (faster ?)
                URI formattedPropertyUri = SPARQLDeserializers.formatURI(property.getUri());

                // don't handle properties which are initially present into SPARQLModel (not managed by this class)
                if (!managedProperties.contains(formattedPropertyUri)) {
                    typesProperties.add(property);
                    propertiesUris.add(formattedPropertyUri);

                    String propertyName = specialCharsPattern.matcher(property.getName()).replaceAll("_");
                    propertiesNames.add(propertyName);
                }
            });

            propertiesByType.put(classModel.getUri(), propertiesUris);
            propertiesByTypeVarNames.put(classModel.getUri(), propertiesNames);
        }
    }


    protected SelectBuilder getSelect() {

        SelectBuilder select = initialSelect.clone();

        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);

        // compute properties SPARQL vars
        List<Var> propertiesVars = new ArrayList<>(typesProperties.size());
        typesProperties.forEach(property -> {

            // ensure that property var is syntactically correct in SPARQL
            String propertyName =  specialCharsPattern.matcher(property.getName()).replaceAll("_");
            propertiesVars.add(makeVar(propertyName));
        });

        // add ?uri var + each property name as SPARQL var
        select.setDistinct(true);
        select.getVars().clear();
        select.addVar(uriVar);
        propertiesVars.forEach(select::addVar);

        // get or create the graph into the query
        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        ElementGroup innerGraphElemGroup;
        if (graph != null) {
            innerGraphElemGroup = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(select.getWhereHandler().getClause(), graph);
        } else {
            innerGraphElemGroup = select.getWhereHandler().getClause();
        }

        Expr propertyBoundednessOurExpr = null;

        // append triple <?uri :property_uri ?property_name> for each property
        int propertyIdx = 0;
        for (PropertyModel property : typesProperties) {

            Node propertyNode = SPARQLDeserializers.nodeURI(property.getUri());
            Var propertyVar = propertiesVars.get(propertyIdx++);
            Triple triple = new Triple(uriVar, propertyNode, propertyVar);

            ElementTriplesBlock elementTriple = new ElementTriplesBlock();
            elementTriple.addTriple(triple);

            /* append the triple as OPTIONAL even if the property is required
                here we supposed that any SPARQL model with any required prop is valid */
            innerGraphElemGroup.addElement(new ElementOptional(elementTriple));

            // build a BOUND filter : only fetch SPARQLModel with at least one relation
            Expr boundPropertyExpr = exprFactory.bound(propertyVar);
            if (propertyBoundednessOurExpr != null) {
                propertyBoundednessOurExpr = exprFactory.or(propertyBoundednessOurExpr, boundPropertyExpr);
            } else {
                propertyBoundednessOurExpr = boundPropertyExpr;
            }
        }

        innerGraphElemGroup.addElementFilter(new ElementFilter(propertyBoundednessOurExpr));

        return select;
    }

    public void updateModels() throws SPARQLException {

        if (typesProperties.isEmpty()) {
            return;
        }

        SelectBuilder selectWithProperties = getSelect();

        AtomicInteger modelsIdx = new AtomicInteger(0);
        AtomicInteger resultsIdx = new AtomicInteger(0);

        sparql.executeSelectQueryAsStream(selectWithProperties).forEach(result -> {

            boolean modelResultMatch = false;
            while (!modelResultMatch) {

                String uriValue = result.getStringValue(SPARQLResourceModel.URI_FIELD);

                if (modelsIdx.get() >= results.size()) {
                    String errorMsg = uriValue +" : URI from SPARQL results (at index"+resultsIdx+") should be included into initial results.";
                    throw new IllegalStateException(errorMsg);
                }
                T model = results.get(modelsIdx.get());
                if (SPARQLDeserializers.compareURIs(model.getUri().toString(), uriValue)) {
                    update(result, model);
                    modelResultMatch = true;
                }else{
                    // no matching with model => try with the next model
                    // only update when no matching, since several result can match with one model (e.g. when they are multiple value for a property)
                    modelsIdx.incrementAndGet();
                }
            }

            resultsIdx.getAndIncrement();
        });
    }

    protected void update(SPARQLResult result, T initialModel) {

        List<URI> typeProperties = this.propertiesByType.get(initialModel.getType());
        List<String> propertiesNames = this.propertiesByTypeVarNames.get(initialModel.getType());

        for (int i = 0; i < typeProperties.size(); i++) {

            String propertyVarName = propertiesNames.get(i);
            String relationValue = result.getStringValue(propertyVarName);

            if (!StringUtils.isEmpty(relationValue)) {
                URI propertyUri = typeProperties.get(i);
                initialModel.addRelation(graphUri, propertyUri, null, relationValue);
            }
        }
    }

}
