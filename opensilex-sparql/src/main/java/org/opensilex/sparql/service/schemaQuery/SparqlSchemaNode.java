/*
 * *****************************************************************************
 *                         SparqlSchemaNode.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.sparql.service.schemaQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.graph.Node;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassAnalyzer;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ClassUtils;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author mhart
 */
public class SparqlSchemaNode<T extends SPARQLResourceModel> extends SparqlSchemaSimpleNode<T>{

    //Final attributes
    private final boolean isListField;
    private final List<SparqlSchemaNode<?>> childNodes;
    private final boolean fetchDynamicRelations;

    //non final variables
    /**
     * Do not call this directly, call getPassedOrDefaultGraph, otherwise a null pointer exception could be thrown
     */
    private Node graph;

    //Constructor with no passed graph
    public SparqlSchemaNode(
            Class<T> objectClass,
            String fieldName,
            List<SparqlSchemaNode<?>> childNodes,
            boolean isListField,
            boolean fetchDynamicRelations
    ) {
        super(objectClass, fieldName);
        this.childNodes = childNodes;
        this.isListField = isListField;
        this.fetchDynamicRelations = fetchDynamicRelations;
    }

    //Constructor to make this node use a specific graph
    public SparqlSchemaNode(
            Class<T> objectClass,
            String fieldName,
            Node graph,
            List<SparqlSchemaNode<?>> childNodes,
            boolean isListField,
            boolean fetchDynamicRelations
    ) {
        super(objectClass, fieldName);
        this.graph = graph;
        this.childNodes = childNodes;
        this.isListField = isListField;
        this.fetchDynamicRelations = fetchDynamicRelations;
    }

    /**
     * Completes the models of this node by looking at each child node, organizing them by type to perform one search for each type.
     * Then recursively performs same operation on child nodes until we reach a node that has no children.
     *
     * @param uncastNodeModels The basic models to complete, they can't be initially passed as T's because the parent node can't access this information
     * @throws Exception
     */
    public void completeNodeModels(
            List<?> uncastNodeModels,
            SPARQLService sparql,
            String lang
    ) throws Exception {
        //If this node has no children or no models were passed, then we have nothing to do here
        if(CollectionUtils.isEmpty(childNodes) || CollectionUtils.isEmpty(uncastNodeModels)) {
            return;
        }

        List<T> nodeModels = uncastNodeModels.stream().map(e -> (T) e).toList();

        //Load all list field's uris that the schema tells us to get, if any
        List<String> listFieldNames = childNodes.stream().filter(SparqlSchemaNode::isListField).map(SparqlSchemaNode::getFieldName).toList();
        if(!CollectionUtils.isEmpty(listFieldNames)){
            SPARQLListFetcher<T> listFetcher = new SPARQLListFetcher<>(
                    sparql,
                    super.getObjectClass(),
                    getPassedOrDefaultGraph(sparql),
                    listFieldNames,
                    nodeModels
            );
            listFetcher.updateModels();
        }

        //Create mapper so we can work out how to get and set fields from field name
        SPARQLClassObjectMapper<T> mapper = sparql.getMapperIndex().getForClass(super.getObjectClass());

        //Calculate the data we need for faster access later
        RecursiveIterationData recursiveIterationData = calculateIterationData(mapper, nodeModels);

        //Do basic search for child models, 1 search per type,
        // to do this iterate over children filling this map as each type is visited for the first time
        //In same iteration perform next recursive step on each child node
        Map<String, Map<String, SPARQLResourceModel>> calculatedChildModelsPerUriPerType = new HashMap<>();

        //In the same way we will fetch all dynamic relations per type when the type is visited for the first time,
        //then complete the models with correct relations after
        Map<String, Map<String, List<SPARQLModelRelation>>> relationsPerUriPerType = new HashMap<>();

        for(SparqlSchemaNode<?> childNode : childNodes){

            ModelsAndRelationsPerUri modelsAndRelations = performSearchesOnTypeAndExtractForField(
                    recursiveIterationData,
                    childNode,
                    calculatedChildModelsPerUriPerType,
                    relationsPerUriPerType,
                    sparql,
                    lang
            );

            //Perform recursive call on child to complete its models, then set their relations.
            if(!MapUtils.isEmpty(modelsAndRelations.modelsPerUriOfCorrectField)){
                performRecursiveCallAndSetRelations(
                        childNode,
                        sparql,
                        modelsAndRelations,
                        lang
                );
            }

            injectCalculatedModels(
                    mapper,
                    childNode,
                    nodeModels,
                    modelsAndRelations,
                    recursiveIterationData
            );

        }

    }

    private void injectCalculatedModels(
            SPARQLClassObjectMapper<T> mapper,
            SparqlSchemaNode<?> childNode,
            List<T> nodeModels,
            ModelsAndRelationsPerUri modelsAndRelations,
            RecursiveIterationData recursiveIterationData
    ) throws InvocationTargetException, IllegalAccessException {

        //Get setter method
        Field field = mapper.getClassAnalyzer().getFieldFromName(childNode.getFieldName());
        Method fieldSetter = mapper.getClassAnalyzer().getSetterFromField(field);

        //Re-inject child models into this node's models
        for(T nodeModel : nodeModels){

            List<SPARQLResourceModel> modelsToSet = new ArrayList<>();
            if(!MapUtils.isEmpty(modelsAndRelations.modelsPerUriOfCorrectField)){
                List<String> uriValues= recursiveIterationData
                        .getUriValuesPerModelUriPerField()
                        .get(childNode.getFieldName())
                        .get(SPARQLDeserializers.getShortURI(nodeModel.getUri()));

                if(!CollectionUtils.isEmpty(uriValues)){
                    uriValues.forEach(uri -> {
                        modelsToSet.add(modelsAndRelations.modelsPerUriOfCorrectField.get(uri));
                    });
                }
            }

            if(CollectionUtils.isEmpty(modelsToSet)){
                continue;
            }

            fieldSetter.invoke(
                    nodeModel,
                    (childNode.isListField ? modelsToSet : modelsToSet.get(0))
            );
        }
    }

    /**
     * Calls the completeNodeModels function and associates correct relations to models in childNode
     *
     * @param childNode to perform recursive call on
     * @param sparql if you don't know what this is you shouldn't be in this class
     * @param modelsAndRelations the ModelsAndRelationsPerUri object that was calculated by performSearchesOnTypeAndExtractForField
     * @param lang language
     * @throws Exception
     */
    private void performRecursiveCallAndSetRelations(
            SparqlSchemaNode<?> childNode,
            SPARQLService sparql,
            ModelsAndRelationsPerUri modelsAndRelations,
            String lang
    ) throws Exception {
        childNode.completeNodeModels(
                new ArrayList<>(modelsAndRelations.modelsPerUriOfCorrectField.values()),
                sparql,
                lang
        );

        if(childNode.fetchDynamicRelations){
            for(Map.Entry<String, SPARQLResourceModel> uriModelEntry : modelsAndRelations.modelsPerUriOfCorrectField.entrySet()){
                String childModelUri = uriModelEntry.getKey();
                List<SPARQLModelRelation> relationsForUri = modelsAndRelations.relationsPerUriOfCorrectField.get(childModelUri);
                if(relationsForUri == null){
                    //If there were no relations then set to an empty list as the dtos expect a non null object
                    relationsForUri = new ArrayList<>();
                }
                uriModelEntry.getValue().setRelations(relationsForUri);
            }
        }
    }

    /**
     * A simple class representing return type of the performSearchesOnTypeAndExtractForField method
     */
    private static class ModelsAndRelationsPerUri{
        final Map<String, SPARQLResourceModel> modelsPerUriOfCorrectField;
        final Map<String, List<SPARQLModelRelation>> relationsPerUriOfCorrectField;

        private ModelsAndRelationsPerUri(Map<String, SPARQLResourceModel> modelsPerUriOfCorrectField, Map<String, List<SPARQLModelRelation>> relationsPerUriOfCorrectField) {
            this.modelsPerUriOfCorrectField = modelsPerUriOfCorrectField;
            this.relationsPerUriOfCorrectField = relationsPerUriOfCorrectField;
        }
    }

    /**
     * Performs a sparql request to get all models, and another to get all relations,
     * for all URIS that have a TYPE matching the type of childNode. Only if they haven't already been gotten.
     * Then extracts the models and relations that are needed for the FIELD of childNode.
     */
    private ModelsAndRelationsPerUri performSearchesOnTypeAndExtractForField(
            RecursiveIterationData recursiveIterationData,
            SparqlSchemaNode<?> childNode,
            Map<String, Map<String, SPARQLResourceModel>> calculatedChildModelsPerUriPerType,
            Map<String, Map<String, List<SPARQLModelRelation>>> relationsPerUriPerType,
            SPARQLService sparql,
            String lang

    ) throws Exception {
        String typeName = recursiveIterationData.getTypeNamePerFieldName().get(childNode.getFieldName());

        //Load models of this child's type if they weren't already loaded
        loadModelsOfTypeIfUnvisited(
                calculatedChildModelsPerUriPerType,
                typeName,
                recursiveIterationData,
                childNode,
                sparql,
                lang
        );

        //Load relations of this type if they were not already loaded
        loadRelationsOfTypeIfUnvisited(
                relationsPerUriPerType,
                typeName,
                recursiveIterationData,
                childNode,
                sparql
        );

        //Extract only the calculated models for childNode's field
        Map<String, SPARQLResourceModel> modelsPerUriOfCorrectField = extractPerFieldMapFromPerTypeMap(
                recursiveIterationData,
                calculatedChildModelsPerUriPerType.get(typeName),
                childNode
        );

        //Extract only the calculated relations for childNode's field
        Map<String, List<SPARQLModelRelation>> relationsPerUriOfCorrectField = extractPerFieldMapFromPerTypeMap(
                recursiveIterationData,
                relationsPerUriPerType.get(typeName),
                childNode
        );

        return new ModelsAndRelationsPerUri(
                modelsPerUriOfCorrectField,
                relationsPerUriOfCorrectField
        );
    }

    /**
     *
     * @param recursiveIterationData
     * @param somethingPerUriOfCorrectType an existing uri -> U map, containing key -value pairs for every field of a certain type
     * @param childNode
     * @param <U> the concerned type that is being mapped
     * @return a uri -> U map, for all models corresponding to the field represented by childNode
     */
    private <U> Map<String, U> extractPerFieldMapFromPerTypeMap(
            RecursiveIterationData recursiveIterationData,
            Map<String, U> somethingPerUriOfCorrectType,
            SparqlSchemaNode<?> childNode
    ){
        HashMap<String, U> relationsPerUriOfCorrectField = new HashMap<>();
        if(MapUtils.isEmpty(somethingPerUriOfCorrectType)){
            return relationsPerUriOfCorrectField;
        }
        recursiveIterationData.getUriValuesPerModelUriPerField()
                .get(childNode.getFieldName())
                .values()
                .forEach((List<String> uriList) -> {
                    uriList.forEach(uri -> relationsPerUriOfCorrectField.put(uri, somethingPerUriOfCorrectType.get(uri)));
                });
        return relationsPerUriOfCorrectField;
    }

    /**
     * Iterates over each field (child node) and each model to organise some things into Maps for faster access afterward.
     *
     * @param mapper used tyo fetch type and getters of fields on a class
     * @param nodeModels to iterate over for each field
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private RecursiveIterationData calculateIterationData(SPARQLClassObjectMapper<T> mapper, List<T> nodeModels) throws InvocationTargetException, IllegalAccessException {
        //Organize all distinct uris per type (regardless of field)
        HashMap<String, HashSet<String>> distinctUrisPerTypeName = new HashMap<>();
        HashMap<String, String> typeNamePerFieldName = new HashMap<>();
        //Map to remember the uri values that the list fetcher got, for faster access later
        HashMap<String, HashMap<String, List<String>>> uriValuesPerModelUriPerField = new HashMap<>();
        //Map to remember the uris per type that we will need to fetch the dynamic relations for
        HashMap<String, HashSet<String>> distinctUrisToDescribePerTypeName = new HashMap<>();

        for(SparqlSchemaNode<?> childNode : childNodes){
            //Identify field, its getter, and the generic type
            // then iterate over each model to place the uris into distinctUrisPerTypeName
            Field field = mapper.getClassAnalyzer().getFieldFromName(childNode.getFieldName());
            //If it is a list field then getType will return java.util.List,
            // so we will instead use ClassUtils which depends on Field.getGenericInfo which is null when it is not a list
            //(ClassUtils.getGenericTypeFromField(field) only works on list fields is this normal?)
            Class<?> genericType = field.getType();
            if(childNode.isListField()){
                genericType = ClassUtils.getGenericTypeFromField(field);
            }
            String genericTypeName = null;

            try{
                genericTypeName = genericType.getTypeName();
            }catch (NullPointerException ignore){}

            if ( genericTypeName == null) {
                throw new IllegalArgumentException("Unknown custom field " + childNode.getFieldName() + " from SPARQL model : " + mapper.getObjectClass().getName());
            }

            typeNamePerFieldName.put(childNode.getFieldName(), genericTypeName);

            Method fieldGetter = mapper.getClassAnalyzer().getGetterFromField(field);

            HashMap<String, List<String>> uriValuesPerModelUri = new HashMap<>();

            for(T nodeModel : nodeModels){
                //Stuff to do with the general maps
                Object uriValueAsObject = fieldGetter.invoke(nodeModel);
                if(uriValueAsObject == null){
                    continue;
                }
                List<String> nextUris = childNode.getUrisFromObject(uriValueAsObject).stream().map(SPARQLDeserializers::getShortURI).toList();

                uriValuesPerModelUri.put(SPARQLDeserializers.getShortURI(nodeModel.getUri()), nextUris);

                if(distinctUrisPerTypeName.containsKey(genericTypeName)){
                    distinctUrisPerTypeName.get(genericTypeName).addAll(nextUris);
                }else{
                    distinctUrisPerTypeName.put(genericTypeName, new HashSet<>(nextUris));
                }

                //Stuff to do with dynamic relations
                if(childNode.fetchDynamicRelations){
                    if(distinctUrisToDescribePerTypeName.containsKey(genericTypeName)){
                        distinctUrisToDescribePerTypeName.get(genericTypeName).addAll(nextUris);
                    }else{
                        distinctUrisToDescribePerTypeName.put(genericTypeName, new HashSet<>(nextUris));
                    }
                }
            }

            uriValuesPerModelUriPerField.put(childNode.getFieldName(), uriValuesPerModelUri);
        }
        RecursiveIterationData result = new RecursiveIterationData();
        result.setDistinctUrisPerTypeName(distinctUrisPerTypeName);
        result.setTypeNamePerFieldName(typeNamePerFieldName);
        result.setUriValuesPerModelUriPerField(uriValuesPerModelUriPerField);
        result.setDistinctUrisToDescribePerTypeName(distinctUrisToDescribePerTypeName);

        return result;
    }

    private void loadModelsOfTypeIfUnvisited(
            Map<String, Map<String, SPARQLResourceModel>> calculatedChildModelsPerUriPerType,
            String typeName,
            RecursiveIterationData recursiveIterationData,
            SparqlSchemaNode<?> nextNode,
            SPARQLService sparql,
            String lang
    ) throws Exception {
        if(calculatedChildModelsPerUriPerType.containsKey(typeName)){
            return;
        }
        HashMap<String, SPARQLResourceModel> calculatedModelsPerUri = new HashMap<>();
        HashSet<String> distinctUris = recursiveIterationData.getDistinctUrisPerTypeName().get(typeName);
        if(!CollectionUtils.isEmpty(distinctUris)){
            nextNode.runBasicSearchFunction(
                    recursiveIterationData.getDistinctUrisPerTypeName().get(typeName),
                    sparql,
                    lang
            ).forEach(e -> calculatedModelsPerUri.put(SPARQLDeserializers.getShortURI(e.getUri()), e));

            calculatedChildModelsPerUriPerType.put(typeName, calculatedModelsPerUri);
        }
    }

    private void loadRelationsOfTypeIfUnvisited(
            Map<String, Map<String, List<SPARQLModelRelation>>> relationsPerUriPerType,
            String typeName,
            RecursiveIterationData recursiveIterationData,
            SparqlSchemaNode<?> nextNode,
            SPARQLService sparql
    ) throws Exception {
        if(!nextNode.fetchDynamicRelations || relationsPerUriPerType.containsKey(typeName)){
            return;
        }

        HashSet<String> distinctUrisToDescribe =
                recursiveIterationData.getDistinctUrisToDescribePerTypeName().get(typeName);

        if(!CollectionUtils.isEmpty(distinctUrisToDescribe)){
            HashMap<String, List<SPARQLModelRelation>> relationsPerUri = getRelationsAndCreateUriRelationsMap(
                    nextNode,
                    distinctUrisToDescribe,
                    sparql
            );

            relationsPerUriPerType.put(typeName, relationsPerUri);
        }
    }

    /**
     *
     * @param node so we know which class to read to remove already handled, non-dynamic relations
     * @param distinctUrisToDescribe
     * @return a uri-relationList map for all dynamic relations found. Public method so it can be used for the root node from SparqlSchema
     * @throws Exception
     */
    public static HashMap<String, List<SPARQLModelRelation>> getRelationsAndCreateUriRelationsMap(
            SparqlSchemaNode<?> node,
            HashSet<String> distinctUrisToDescribe,
            SPARQLService sparql
    ) throws Exception {
        HashMap<String, List<SPARQLModelRelation>> relationsPerUri = new HashMap<>();
        //Fetch dynamic relations where the uris to describe are subjects
        if(!CollectionUtils.isEmpty(distinctUrisToDescribe)){
            node.runRelationFetchingFunction(
                    distinctUrisToDescribe,
                    sparql,
                    true
            ).forEach(statement -> {
                addToRelationsPerUri(
                        relationsPerUri,
                        statement,
                        true
                );
            });
            //Fetch dynamic relations where the uris to describe are objects
            node.runRelationFetchingFunction(
                    distinctUrisToDescribe,
                    sparql,
                    false
            ).forEach(statement -> {
                addToRelationsPerUri(
                        relationsPerUri,
                        statement,
                        false
                );
            });
        }

        return relationsPerUri;
    }

    /**
     * Function to create a SPARQLModelRelation from a statement, then add to map
     *
     * @param relationsPerUri the map to modify
     * @param statement the sparql statement to read from
     * @param subjectIsConcernedUri if true then not a reverse relation, else it is
     */
    private static void addToRelationsPerUri(
            HashMap<String, List<SPARQLModelRelation>> relationsPerUri,
            SPARQLStatement statement,
            boolean subjectIsConcernedUri
    ){
        SPARQLModelRelation nextRelation = new SPARQLModelRelation();

        nextRelation.setReverse(!subjectIsConcernedUri);

        String currentConcernedUri = SPARQLDeserializers.getShortURI(statement.getSubject());

        if(subjectIsConcernedUri){
            nextRelation.setValue(statement.getObject());
        }else{
            nextRelation.setValue(statement.getSubject());
            currentConcernedUri = SPARQLDeserializers.getShortURI(statement.getObject());
        }

        nextRelation.setProperty(Ontology.property(statement.getPredicate()));

        List<SPARQLModelRelation> listOfRelations = relationsPerUri.get(currentConcernedUri);
        if(listOfRelations==null){
            listOfRelations = new ArrayList<>();
        }
        listOfRelations.add(nextRelation);

        relationsPerUri.put(currentConcernedUri, listOfRelations);
    }

    /**
     * A function to perform a search for this node's generic type on the passed list of uris.
     * Called from a parent to perform a search for all it's children of same type.
     * Has to be done in the child as we need to know the type.
     *
     * @param uris
     * @return a list of distinct models, we know they will be distinct because we pass a distinct uri list,
     * we return a list of models instead of a set because the search function returns a list and the conversion
     * from list to set was sometimes creating issues because it uses the .equals function.
     */
    private List<? extends SPARQLResourceModel> runBasicSearchFunction(
            HashSet<String> uris,
            SPARQLService sparql,
            String lang
    ) throws Exception {
        SparqlNoProxyFetcher<T> customFetcher = new SparqlNoProxyFetcher<>(
                super.getObjectClass(),
                sparql
        );

        //Call normal search function
        return sparql.search(
                getPassedOrDefaultGraph(sparql),
                super.getObjectClass(),
                lang,
                (SelectBuilder select) -> {
                    select.addFilter(SPARQLQueryHelper.inURIFilter(
                            SPARQLResourceModel.URI_FIELD,
                            uris.stream().map(URI::create).toList()
                    ));
                },
                null,
                (SPARQLResult result) -> customFetcher.getInstance(result, lang),
                Collections.emptyList(),
                0,
                0
        );
    }

    /**
     *
     * @param object that can be cast to either a list of T or a single T in function of isListField
     * @return a list of all the uris from the cast model(s)
     */
    private List<URI> getUrisFromObject(Object object){
        if(isListField){
            return ((List<T>)object).stream().map(SPARQLResourceModel::getUri).toList();
        }
        return Collections.singletonList(((T)object).getUri());
    }

    /**
     *
     * @param uris of objects to fetch dynamic relations for
     * @param sparql service
     * @param urisAreSubjects if true then get relations where uris are subjects, else relations where uris are objects
     * @return dynamic relations where uris are subjects or objects in function of urisAreSubjects boolean,
     * does this by running a construct query then filtering out any triplets that are basic managed properties
     * @throws Exception
     */
    private List<SPARQLStatement> runRelationFetchingFunction(
            HashSet<String> uris,
            SPARQLService sparql,
            boolean urisAreSubjects
    ) throws Exception {
        List<String> propertiesToIgnore = sparql.getMapperIndex().getForClass(super.getObjectClass()).getClassAnalyzer().getManagedProperties()
                .stream().map(Property::getURI).toList();

        ConstructBuilder constructBuilder = new ConstructBuilder();

        Var subjectVar = makeVar("s");
        Var predicateVar = makeVar("p");
        Var objectVar = makeVar("o");

        constructBuilder.addConstruct(subjectVar, predicateVar, objectVar);

        WhereBuilder innerWhere = new WhereBuilder()
                .addGraph(getPassedOrDefaultGraph(sparql), subjectVar, predicateVar, objectVar);

        if(urisAreSubjects){
            SPARQLQueryHelper.addWhereUriStringValues(innerWhere, "s", uris.stream(), true, uris.size());
        }else{
            SPARQLQueryHelper.addWhereUriStringValues(innerWhere, "o", uris.stream(), true, uris.size());
        }

        constructBuilder.addWhere(innerWhere);

        return sparql.executeConstructQuery(constructBuilder).stream()
                .filter(e -> !propertiesToIgnore.contains(e.getPredicate())).toList();

    }

    private Node getPassedOrDefaultGraph(SPARQLService sparql) throws SPARQLException {
        if(this.graph==null){
            this.graph = sparql.getDefaultGraph(super.getObjectClass());
        }
        return graph;
    }

    public boolean isListField() {
        return isListField;
    }

    public boolean isFetchDynamicRelations() {
        return fetchDynamicRelations;
    }

    /**
     * Class to store all the maps we use for faster access during a completeNodeModels iteration
     */
    private static class RecursiveIterationData{

        private HashMap<String, HashSet<String>> distinctUrisPerTypeName = new HashMap<>();
        private HashMap<String, String> typeNamePerFieldName = new HashMap<>();
        private HashMap<String, HashMap<String, List<String>>> uriValuesPerModelUriPerField = new HashMap<>();
        private HashMap<String, HashSet<String>> distinctUrisToDescribePerTypeName = new HashMap<>();

        public HashMap<String, HashSet<String>> getDistinctUrisPerTypeName() {
            return distinctUrisPerTypeName;
        }

        public void setDistinctUrisPerTypeName(HashMap<String, HashSet<String>> distinctUrisPerTypeName) {
            this.distinctUrisPerTypeName = distinctUrisPerTypeName;
        }

        public HashMap<String, String> getTypeNamePerFieldName() {
            return typeNamePerFieldName;
        }

        public void setTypeNamePerFieldName(HashMap<String, String> typeNamePerFieldName) {
            this.typeNamePerFieldName = typeNamePerFieldName;
        }

        public HashMap<String, HashMap<String, List<String>>> getUriValuesPerModelUriPerField() {
            return uriValuesPerModelUriPerField;
        }

        public void setUriValuesPerModelUriPerField(HashMap<String, HashMap<String, List<String>>> uriValuesPerModelUriPerField) {
            this.uriValuesPerModelUriPerField = uriValuesPerModelUriPerField;
        }

        public HashMap<String, HashSet<String>> getDistinctUrisToDescribePerTypeName() {
            return distinctUrisToDescribePerTypeName;
        }

        public void setDistinctUrisToDescribePerTypeName(HashMap<String, HashSet<String>> distinctUrisToDescribePerTypeName) {
            this.distinctUrisToDescribePerTypeName = distinctUrisToDescribePerTypeName;
        }
    }
}
