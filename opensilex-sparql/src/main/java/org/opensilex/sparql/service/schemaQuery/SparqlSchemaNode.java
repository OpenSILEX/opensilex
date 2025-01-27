package org.opensilex.sparql.service.schemaQuery;

import org.apache.commons.collections.CollectionUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLListFetcher;
import org.opensilex.sparql.mapping.SparqlNoProxyFetcher;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLStatement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class SparqlSchemaNode<T extends SPARQLResourceModel>{

    private final Class<T> objectClass;
    private final String fieldName;
    private final boolean isListField;
    private final List<SparqlSchemaNode<?>> childNodes;
    private final boolean fetchDynamicRelations;

    public SparqlSchemaNode(
            Class<T> objectClass,
            String fieldName,
            List<SparqlSchemaNode<?>> childNodes,
            boolean isListField,
            boolean fetchDynamicRelations
    ) {
        this.objectClass = objectClass;
        this.fieldName = fieldName;
        this.childNodes = childNodes;
        this.isListField = isListField;
        this.fetchDynamicRelations = fetchDynamicRelations;
    }

    /**
     * Completes the models of this node by looking at each child node, organizing them by type to perform one search for each type.
     * Then recursively performs same operation on child nodes until we reach a node that has no children.
     *
     * @param sparql the sparql service, needed to perform basic search for each type found among the child nodes
     * @param uncastNodeModels The basic models to complete, they can't be initially passed as T's because the parent node can't access this information
     * @param lang language of string fields returned inside models
     * @throws Exception
     */
    public void completeNodeModels(
            SPARQLService sparql,
            List<?> uncastNodeModels,
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
                    objectClass,
                    sparql.getDefaultGraph(objectClass),
                    listFieldNames,
                    nodeModels
            );
            listFetcher.updateModels();
        }

        //Create mapper so we can work out how to get and set fields from field name
        SPARQLClassObjectMapper<T> mapper = sparql.getMapperIndex().getForClass(objectClass);

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
            Class<?> genericType = field.getType();
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
                List<String> nextUris = childNode.getUrisFromObject(fieldGetter.invoke(nodeModel)).stream().map(URI::toString).toList();

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

        //Do basic search for child models, 1 search per type,
        // to do this iterate over children filling this map as each type is visited for the first time
        //In same iteration perform next recursive step on each child node
        HashMap<String, HashMap<String, SPARQLResourceModel>> calculatedChildModelsPerUriPerType = new HashMap<>();
        //In the same way we will fetch all dynamic relations per type when the type is visited for the first time,
        //then complete the models with correct relations after
        HashMap<String, HashMap<String, List<SPARQLStatement>>> relationsPerUriPerType = new HashMap<>();

        for(SparqlSchemaNode<?> childNode : childNodes){
            String typeName = typeNamePerFieldName.get(childNode.getFieldName());
            //Load models of this child's type if they weren't already loaded
            if(!calculatedChildModelsPerUriPerType.containsKey(typeName)){
                HashMap<String, SPARQLResourceModel> calculatedModelsPerUri = new HashMap<>();

                childNode.runBasicSearchFunction(
                        distinctUrisPerTypeName.get(typeName),
                        sparql,
                        lang
                ).forEach(e -> calculatedModelsPerUri.put(SPARQLDeserializers.getShortURI(e.getUri()), e));
                calculatedChildModelsPerUriPerType.put(typeName, calculatedModelsPerUri);
            }
            //Load relations of this type if they were not already loaded
            if(childNode.fetchDynamicRelations && !relationsPerUriPerType.containsKey(typeName)){
                HashMap<String, List<SPARQLStatement>> relationsPerUri = new HashMap<>();

                /*List<SPARQLStatement> dynamicRelations = */childNode.runRelationFetchingFunction(
                        distinctUrisToDescribePerTypeName.get(typeName),
                        sparql
                ).forEach(e -> {

                    relationsPerUri.put(SPARQLDeserializers.getShortURI(e.getUri()), e)
                });


                calculatedChildModelsPerUriPerType.put(typeName, calculatedModelsPerUri);


            }

            //Perform recursive call on child to complete its models
            childNode.completeNodeModels(
                    sparql,
                    new ArrayList<>(calculatedChildModelsPerUriPerType.get(typeName).values()),
                    lang
            );
            //Re-inject child models into this node's models
            //Get setter method
            Field field = mapper.getClassAnalyzer().getFieldFromName(childNode.getFieldName());
            Method fieldSetter = mapper.getClassAnalyzer().getSetterFromField(field);
            for(T nodeModel : nodeModels){

                HashMap<String, SPARQLResourceModel> modelsOfCorrectType = calculatedChildModelsPerUriPerType.get(typeName);
                List<SPARQLResourceModel> modelsToSet = new ArrayList<>();
                uriValuesPerModelUriPerField.get(childNode.getFieldName()).get(SPARQLDeserializers.getShortURI(nodeModel.getUri())).forEach(uri -> {
                    modelsToSet.add(modelsOfCorrectType.get(uri));
                });

                if(CollectionUtils.isEmpty(modelsToSet)){
                    continue;
                }

                fieldSetter.invoke(
                        nodeModel,
                        (childNode.isListField ? modelsToSet : modelsToSet.get(0))
                );
            }
        }

    }

    /**
     * A function to perform a search for this node's generic type on the passed list of uris.
     * Called from a parent to perform a search for all it's children of same type.
     * Has to be done in the child as we need to know the type.
     *
     * @param uris
     * @return
     */
    private HashSet<? extends SPARQLResourceModel> runBasicSearchFunction(
            HashSet<String> uris,
            SPARQLService sparql,
            String lang
    ) throws Exception {
        SparqlNoProxyFetcher<T> customFetcher = new SparqlNoProxyFetcher<>(
                objectClass,
                sparql
        );

        //Call normal search function
        List<? extends SPARQLResourceModel> nextModels = sparql.search(
                sparql.getDefaultGraph(objectClass),
                objectClass,
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
        return new HashSet<>(nextModels);
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
     * @return dynamic relations, does this by describing the uris then filtering out any triplets that are basic managed properties
     * @throws Exception
     */
    private List<SPARQLStatement> runRelationFetchingFunction(
            HashSet<String> uris,
            SPARQLService sparql
    ) throws Exception {
        List<String> propertiesToIgnore = sparql.getMapperIndex().getForClass(objectClass).getClassAnalyzer().getManagedProperties()
                .stream().map(Property::getURI).toList();
        return sparql.describeMany(
                sparql.getDefaultGraph(objectClass),
                new HashSet<>(uris.stream().map(URI::create).toList())
        ).stream().filter(e -> !propertiesToIgnore.contains(e.getPredicate())).collect(Collectors.toList());
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isListField() {
        return isListField;
    }
}
