//******************************************************************************
//                               AnnotationDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import opensilex.service.dao.exception.DAOPersistenceException;

import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DateFormats;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAODataErrorException;
import opensilex.service.dao.exception.UnknownUriException;
import opensilex.service.dao.exception.WrongTypeException;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.User;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oa;
import opensilex.service.ontology.Oeso;
import opensilex.service.utils.sparql.SPARQLQueryBuilder;
import opensilex.service.utils.JsonConverter;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.date.Dates;
import opensilex.service.model.Annotation;

/**
 * Annotations DAO.
 * @update [Andréas Garcia] 15 Feb. 2019: search parameters are no longer class 
 * attributes but parameters sent through search functions
 * @update [Andréas Garcia] 8 Apr. 2019: Use DAO generic function create, update, checkBeforeCreation and use exceptions 
 * to handle errors.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AnnotationDAO extends Rdf4jDAO<Annotation> {

    final static Logger LOGGER = LoggerFactory.getLogger(AnnotationDAO.class);

    // constants used for SPARQL names in the SELECT statement
    public static final String CREATED = "created";
    public static final String BODY_VALUE = "bodyValue";
    public static final String BODY_VALUES = "bodyValues";
    public static final String CREATOR = "creator";
    public static final String TARGET = "target";
    public static final String TARGETS = "targets";
    public static final String MOTIVATED_BY = "motivatedBy";

    public AnnotationDAO() {
        super();
    }

    public AnnotationDAO(User user) {
        super(user);
    }
    
    /**
     * Generates a query to search the body values of annotations.
     * @param annotations
     * @example
     * SELECT  ?uri ?bodyValue WHERE {
     *      ?uri  <http://www.w3.org/ns/oa#bodyValue>  ?bodyValue  .
     *      FILTER (
     *          (regex(str(?uri), "http://www.opensilex.org/andreas-dev/id/annotation/7dfcd6e1-bc6e-4553-85c7-295ab971f2fc")) 
     *          || (regex(str(?uri), "http://www.opensilex.org/andreas-dev/id/annotation/0e92c41b-93df-4395-bc24-6e5373022be7"))
     *      ) 
     * }
     * @return query generated with the searched parameter above
     */
    private SPARQLQueryBuilder prepareSearchQueryForBodyValues(List<Annotation> annotations) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();
        
        query.appendSelect(URI_SELECT_NAME_SPARQL);
        query.appendSelect("?" + BODY_VALUE);
        
        annotations.forEach(annotation -> {
            query.appendOrFilter("regex(str(" + URI_SELECT_NAME_SPARQL + ")" + ", \"" + annotation.getUri() + "\")");
        });
        query.appendTriplet(URI_SELECT_NAME_SPARQL, Oa.RELATION_BODY_VALUE.toString(), "?" + BODY_VALUE, null);
        
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }
    
    /**
     * Query generated with the searched parameters. 
     * It doesn't return body values but it applies a body value filter if provided.
     * @param uri
     * @param creator
     * @param target
     * @param bodyValue
     * @param motivatedBy
     * @param dateSortAsc
     * @example
     * SELECT DISTINCT ?uri 
     * WHERE { 
     *   ?uri <http://purl.org/dc/terms/creationDate> ?creationDate . 
     *   ?uri <http://purl.org/dc/terms/creator> ?creator .
     *   ?uri <http://www.w3.org/ns/oa#motivatedBy> ?motivatedBy . 
     *   ?uri <http://www.w3.org/ns/oa#bodyValue> ?bodyValue . } 
     * LIMIT 20
     * @return query generated with the searched parameter above
     */
    private SPARQLQueryBuilder prepareSearchQueryWithoutBodyValues(String uri, String creator, String target, String bodyValue, String motivatedBy, boolean dateSortAsc) {
        SPARQLQueryBuilder query = new SPARQLQueryBuilder();

        String annotationUri;
        if (uri != null) {
            annotationUri = "<" + uri + ">";
        } else {
            annotationUri = "?" + URI;
            query.appendSelect(annotationUri);
            query.appendGroupBy(annotationUri);
        }
        
        query.appendSelect("?" + CREATED);
        query.appendGroupBy("?" + CREATED);
        query.appendTriplet(annotationUri, DCTerms.created.getURI(), "?" + CREATED, null);

        if (creator != null) {
            query.appendTriplet(annotationUri, DCTerms.creator.getURI(), creator, null);
        } else {
            query.appendSelect("?" + CREATOR);
            query.appendGroupBy("?" + CREATOR);
            query.appendTriplet(annotationUri, DCTerms.creator.getURI(), "?" + CREATOR, null);
        }

        if (motivatedBy != null) {
            query.appendTriplet(annotationUri, Oa.RELATION_MOTIVATED_BY.toString(), motivatedBy, null);
        } else {
            query.appendSelect("?" + MOTIVATED_BY);
            query.appendGroupBy("?" + MOTIVATED_BY);
            query.appendTriplet(annotationUri, Oa.RELATION_MOTIVATED_BY.toString(), "?" + MOTIVATED_BY, null);
        }

        query.appendSelectConcat("?" + TARGET, SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR, "?" + TARGETS);
        query.appendTriplet(annotationUri, Oa.RELATION_HAS_TARGET.toString(), "?" + TARGET, null);
        if (target != null) {
            query.appendTriplet(annotationUri, Oa.RELATION_HAS_TARGET.toString(), target, null);
        }

        query.appendTriplet(annotationUri, Oa.RELATION_BODY_VALUE.toString(), "?" + BODY_VALUE, null);
        if (bodyValue != null) {
            query.appendFilter("regex(STR(?" + BODY_VALUE + "), '" + bodyValue + "', 'i')");
        }
        query.appendLimit(this.getPageSize());
        query.appendOffset(this.getPage() * this.getPageSize()); 
        if(dateSortAsc){
            query.appendOrderBy("ASC(?" + CREATED + ")");
        } else {
             query.appendOrderBy("DESC(?" + CREATED + ")");
        }
        LOGGER.debug(SPARQL_QUERY + query.toString());
        return query;
    }

    /**
     * @param searchUri
     * @param searchCreator
     * @param searchTarget
     * @param searchBodyValue
     * @param searchMotivatedBy
     * @param dateSortAsc
     * @return number of total annotation returned with the search field
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    public Integer count(String searchUri, String searchCreator, String searchTarget, String searchBodyValue, String searchMotivatedBy, boolean dateSortAsc) 
            throws DAOPersistenceException, Exception {
        SPARQLQueryBuilder prepareCount = prepareCount(
                searchUri, 
                searchCreator, 
                searchTarget, 
                searchBodyValue, 
                searchMotivatedBy,
                dateSortAsc);
        TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, prepareCount.toString());
        Integer count = 0;
        try {
            TupleQueryResult result = tupleQuery.evaluate();
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                count = Integer.parseInt(bindingSet.getValue(COUNT_ELEMENT_QUERY).stringValue());
            }
        }
        catch (QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        catch (NumberFormatException ex) {
            handleCountValueNumberFormatException(ex);
        }
        return count;
    }

    /**
     * Counts query generated by the searched parameters.
     * @example
     * SELECT (count(distinct ?uri) as ?count) 
     * WHERE { 
     *   ?uri <http://purl.org/dc/terms/creationDate> ?creationDate . 
     *   ?uri <http://purl.org/dc/terms/creator>
     *   <http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy> . 
     *   ?uri <http://www.w3.org/ns/oa#motivatedBy> <http://www.w3.org/ns/oa#commenting> . 
     *   ?uri <http://www.w3.org/ns/oa#bodyValue> ?bodyValue . 
     * FILTER (regex(STR(?bodyValue), 'Ustilago maydis infection', 'i') ) 
     * }
     * @return query generated with the searched parameters
     */
    private SPARQLQueryBuilder prepareCount(String searchUri, String searchCreator, String searchTarget, String searchBodyValue, String searchMotivatedBy, boolean dateSortAsc) {
        SPARQLQueryBuilder query = prepareSearchQueryWithoutBodyValues(
                searchUri, 
                searchCreator, 
                searchTarget, 
                searchBodyValue, 
                searchMotivatedBy,
                dateSortAsc);
        query.clearSelect();
        query.clearLimit();
        query.clearOffset();
        query.clearGroupBy();
        query.clearOrderBy();
        query.appendSelect("(COUNT(DISTINCT ?" + URI + ") AS ?" + COUNT_ELEMENT_QUERY + ")");
        LOGGER.debug(SPARQL_QUERY + " " + query.toString());
        return query;
    }
    
    /**
     * Sets generated URIs to annotations.
     * @param annotations
     * @throws Exception 
     */
    public static void setNewUris (List<Annotation> annotations) throws Exception {
        for (Annotation annotation : annotations) {
            annotation.setUri(UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_ANNOTATION.toString(), null, null));
        }
    }

    /**
     * Inserts the given annotations in the storage.
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     * @note annotations have to be checked before calling this function.
     * @param annotations
     * @return the insertion resultAnnotationUri, with the errors list or the
     * URI of the inserted annotations
     * @throws opensilex.service.dao.exception.SemanticInconsistencyException
     * @throws opensilex.service.dao.exception.UnknownUriException
     */
    @Override
    public List<Annotation> create(List<Annotation> annotations) throws DAOPersistenceException, Exception {
        setNewUris(annotations);
        UpdateBuilder updateBuilder = new UpdateBuilder();
        addInsertToUpdateBuilder(updateBuilder, annotations); 
        executeUpdateRequest(updateBuilder);
        return annotations;
    }

    /**
     * Adds statements to an update builder to insert an annotation. 
     * @param annotations
     * @example
     * INSERT DATA {
     *  <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37> rdf:type  <http://www.w3.org/ns/oa#Annotation> .
     *  <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37> <http://purl.org/dc/terms/creationDate> "2018-06-22 15:18:13+0200"^^xsd:dateTime .
     *  <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37> <http://purl.org/dc/terms/creator> http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy> .
     *  <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37> <http://www.w3.org/ns/oa#bodyValue> "Ustilago maydis infection" .
     *  <http://www.phenome-fppn.fr/platform/id/annotation/a2f9674f-3e49-4a02-8770-e5a43a327b37> <http://www.w3.org/ns/oa#hasTarget> <http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy> . 
     * @param updateBuilder
     */
    public static void addInsertToUpdateBuilder(UpdateBuilder updateBuilder, List<Annotation> annotations) {
        
        Node graph = NodeFactory.createURI(Contexts.ANNOTATIONS.toString());
        Node annotationConcept = NodeFactory.createURI(Oeso.CONCEPT_ANNOTATION.toString());
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DateFormats.YMDTHMSZ_FORMAT);
        Property relationMotivatedBy = ResourceFactory.createProperty(Oa.RELATION_MOTIVATED_BY.toString());
        Property relationBodyValue = ResourceFactory.createProperty(Oa.RELATION_BODY_VALUE.toString());
        Property relationHasTarget = ResourceFactory.createProperty(Oa.RELATION_HAS_TARGET.toString());
        
        annotations.forEach((annotation) -> {
            Resource annotationUri = ResourceFactory.createResource(annotation.getUri());
            updateBuilder.addInsert(graph, annotationUri, RDF.type, annotationConcept);
            Literal creationDate = ResourceFactory.createTypedLiteral(
                    annotation.getCreated().toString(formatter), 
                    XSDDatatype.XSDdateTime);
            updateBuilder.addInsert(graph, annotationUri, DCTerms.created, creationDate);

            Node creator =  NodeFactory.createURI(annotation.getCreator());
            updateBuilder.addInsert(graph, annotationUri, DCTerms.creator, creator);

            Node motivatedByReason =  NodeFactory.createURI(annotation.getMotivatedBy());
            updateBuilder.addInsert(graph, annotationUri, relationMotivatedBy, motivatedByReason);

            /**
             * @link https://www.w3.org/TR/annotation-model/#bodies-and-targets
             */
            if (annotation.getBodyValues() != null && !annotation.getBodyValues().isEmpty()) {
                annotation.getBodyValues().forEach((annotbodyValue) -> {
                    updateBuilder.addInsert(graph, annotationUri, relationBodyValue, annotbodyValue);
                });
            }
            /**
             * @link https://www.w3.org/TR/annotation-model/#bodies-and-targets
             */
            if (annotation.getTargets() != null && !annotation.getTargets().isEmpty()) {
                for (String targetUri : annotation.getTargets()) {
                    Resource targetResourceUri = ResourceFactory.createResource(targetUri);
                    updateBuilder.addInsert(graph, annotationUri, relationHasTarget, targetResourceUri);
                }
            }
        });
    }

    /**
     * Checks the given annotations.
     * @param annotations
     * @throws opensilex.service.dao.exception.DAODataErrorAggregateException
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    @Override
    public void validate(List<Annotation> annotations) throws DAODataErrorAggregateException, DAOPersistenceException {
        UriDAO uriDao = new UriDAO();
        UserDAO userDao = new UserDAO();
        ArrayList<DAODataErrorException> exceptions = new ArrayList<>();
        try {
            annotations.forEach((annotation) -> {
                // check motivation
                if (!uriDao.existUri(annotation.getMotivatedBy())) {
                    exceptions.add(new UnknownUriException(annotation.getMotivatedBy(), "the motivation"));
                }
                else if (!uriDao.isInstanceOf(annotation.getMotivatedBy(), Oa.CONCEPT_MOTIVATION.toString())) {
                    exceptions.add(new WrongTypeException(annotation.getMotivatedBy(), "the motivation"));
                }

                // check person
                if (!userDao.existUserUri(annotation.getCreator())) {
                    exceptions.add(new UnknownUriException(annotation.getCreator(), "the person"));
                }

                // check target
                annotation.getTargets().forEach((targetUri) -> {
                    if (!uriDao.existUri(targetUri)) {
                        exceptions.add(new UnknownUriException(targetUri, "the target"));
                    }
                });
            });
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        
        if (exceptions.size() > 0) {
            throw new DAODataErrorAggregateException(exceptions);
        }
    }

    /**
     * Searches all the annotations corresponding to the search parameters.
     * @param uri
     * @param creator
     * @param target
     * @param page
     * @param bodyValue
     * @param motivatedBy
     * @param pageSize
     * @return the list of the annotations found
     * @throws opensilex.service.dao.exception.DAOPersistenceException
     */
    public ArrayList<Annotation> find(String uri, String creator, String target, String bodyValue, String motivatedBy, boolean dateSortAsc, int page, int pageSize) 
            throws DAOPersistenceException {
        setPage(page);
        setPageSize(pageSize);

        // retrieve URI list
        SPARQLQueryBuilder query = prepareSearchQueryWithoutBodyValues(
                uri, 
                creator, 
                target, 
                bodyValue, 
                motivatedBy,
                dateSortAsc);
        ArrayList<Annotation> annotations = null;
        try {
            TupleQuery tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());

            // Retreive all information for each annotation
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                annotations = getAnnotationsWithoutBodyValuesFromResult(result, uri, creator, motivatedBy);
                if(annotations.size() > 0) {
                    query = prepareSearchQueryForBodyValues(annotations);
                    tupleQuery = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query.toString());
                    setAnnotationsBodyValuesFromResult(tupleQuery.evaluate(), annotations);
                }
            }
            LOGGER.debug(JsonConverter.ConvertToJson(annotations));
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        return annotations;
    }

    /**
     * Set the body values of annotations from a query result.
     * @param result
     * @param annotations 
     */
    private void setAnnotationsBodyValuesFromResult(TupleQueryResult result, List<Annotation> annotations) {

        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            
            String annotationUri = getStringValueOfSelectNameFromBindingSet(URI, bindingSet);
            String bodyValue = getStringValueOfSelectNameFromBindingSet(BODY_VALUE, bindingSet);
            
            annotations.stream()
                    .filter(annotation -> annotationUri.equals(annotation.getUri()))
                    .findFirst()
                    .get()
                    .addBodyValue(bodyValue);                    
        }
    }

    /**
     * Gets an annotation result from a given resultAnnotationUri. 
     * @param result a list of annotation from a search query
     * @param searchUri search URI
     * @param searchCreator search creator
     * @param searchMotivatedBy search motivated by
     * @return annotations with data extracted from the given bindingSets
     */
    private ArrayList<Annotation> getAnnotationsWithoutBodyValuesFromResult(TupleQueryResult result, String searchUri, String searchCreator, String searchMotivatedBy) {
        ArrayList<Annotation> annotations = new ArrayList<>();
        UriDAO uriDao = new UriDAO();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
       
            String annotationUri = null;
            if (searchUri != null) {
                if(uriDao.existUri(searchUri)){
                    annotationUri = searchUri;
                }
            } else {
                if(bindingSet.getValue(URI) != null){
                    annotationUri = bindingSet.getValue(URI).stringValue();
                }
            }
            //SILEX:info
            // This test is made because group concat function in the query can create empty row
            // e.g.
            // Uri Created Creator MotivatedBy Targets	BodyValues
            //                                 ""       ""
            //\SILEX:info
            if (annotationUri != null) {
                
                DateTime annotationCreated;
                // creationDate date
                String creationDate = bindingSet.getValue(CREATED).stringValue();
                annotationCreated = Dates.stringToDateTimeWithGivenPattern(creationDate, DateFormats.YMDTHMSZ_FORMAT);

                String annotationCreator;
                if (searchCreator != null) {
                    annotationCreator = searchCreator;
                } else {
                    annotationCreator = bindingSet.getValue(CREATOR).stringValue();
                }

                String annotationMotivation;
                if (searchMotivatedBy != null) {
                    annotationMotivation = searchMotivatedBy;
                } else {
                    annotationMotivation = bindingSet.getValue(MOTIVATED_BY).stringValue();
                }

                //SILEX:info
                // concat query return a list with comma separated value in one column.
                // An annotation has a least one target.
                //\SILEX:info
                ArrayList<String> annotationTargets = new ArrayList<>(Arrays.asList(bindingSet
                        .getValue(TARGETS)
                        .stringValue()
                        .split(SPARQLQueryBuilder.GROUP_CONCAT_SEPARATOR)));

                annotations.add(new Annotation(
                        annotationUri, 
                        annotationCreated, 
                        annotationCreator, 
                        null, 
                        annotationMotivation, 
                        annotationTargets));
            }
        }
        return annotations;
    }
    
   /**
    * @return an {@link UpdateBuilder} producing a SPARQL query which remove all annotation having only 
    * the given annotation as target. 
    * @example 
    * DELETE { ?s ?p ?o . } WHERE { 
    *     ?s oa:hasTarget+ "http://www.phenome-fppn.fr/test/id/annotation/1e331ca4-2e63-4728-8373-050a2b51c3dc". <br>
    *     ?s ?p ?o  
    *     MINUS { 
    *     	  ?s oa:hasTarget ?s2, ?s3 . 
    *     	  FILTER( ?s2 != ?s3) 
    * 	  } 
    * } 
    * @param annotationUri 
    * @throws RepositoryException
    * @throws UpdateExecutionException
    */
    protected UpdateBuilder getRemoveAllSuperAnnotationQuery(String annotationUri) throws RepositoryException, UpdateExecutionException {
    	  	
    	// create variables and annotation ressource
    	Node s = NodeFactory.createVariable("s"), s2 = NodeFactory.createVariable("s2"), s3 = NodeFactory.createVariable("s3"),
    		 p = NodeFactory.createVariable("p"),  o = NodeFactory.createVariable("o"),
    		 oaTargetPred = NodeFactory.createURI(Oa.RELATION_HAS_TARGET.toString()),
    		 annotationNode = NodeFactory.createURI(annotationUri);
    	
    	Path oaTargetPath = PathFactory.pathOneOrMore1(PathFactory.pathLink(oaTargetPred)); // create the property path (oa:target)+ 	
    	
    	return new UpdateBuilder() // build the query
    	   .addDelete(s, p, o) 
		   .addWhere(new TriplePath(s, oaTargetPath, annotationNode) )// add the two WHERE basic graph pattern ( b.g.p.) 
		   .addWhere(s, p, o)
		   .addMinus(new WhereBuilder() // add the minus clause in order to check if the annotation has more than one target 
		   .addWhere(s, oaTargetPred, s2)
		   .addWhere(s, oaTargetPred, s3)
		   .addFilter(new ExprFactory().ne(s2, s3)) // create ?s2 != ?s3, 
		);    
    }
    
    /**
     * @return an {@link UpdateBuilder} producing a SPARQL query which remove all annotation triples
     * @example 
     * DELETE { 
     * 		http://www.phenome-fppn.fr/test/id/annotation/1e331ca4-2e63-4728-8373-050a2b51c3dc ?p ?o .  
     * 		?s ?p1 http://www.phenome-fppn.fr/test/id/annotation/1e331ca4-2e63-4728-8373-050a2b51c3dc   
     * } WHERE {  
     * 		{ http://www.phenome-fppn.fr/test/id/annotation/1e331ca4-2e63-4728-8373-050a2b51c3dc ?p ?o }  
     * 		UNION  
     * 		{?s ?p1 http://www.phenome-fppn.fr/test/id/annotation/1e331ca4-2e63-4728-8373-050a2b51c3dc }  
     * }  
     * 
     * @param annotationUri : the URI of the {@link Annotation} to delete
     */
    protected UpdateBuilder getRemoveAllAnnotationTripleQuery(String annotationUri) throws RepositoryException {
    	
    	// create variables and annotation ressource
    	Node  s = NodeFactory.createVariable("s"),
    		  p = NodeFactory.createVariable("p"),
    		  p2 = NodeFactory.createVariable("p2"),
       		  o = NodeFactory.createVariable("o"), 
       		  annotationNode = NodeFactory.createURI(annotationUri);
    	
    	return new UpdateBuilder()
    		.addDelete(annotationNode,p,o)
    		.addDelete(s,p2,annotationNode)
    		.addWhere(annotationNode, p, o) // add the <s,p,annotation_uri> UNION <annotation_uri,p,o>
			.addUnion(new WhereBuilder().addWhere(s,p2,annotationNode)
		);   		
    }
    
    /**
     * @apiNote
     * WARNING : delete an annotation trigger the deletion of all annotation which only have the annotation as target . 
     */
    @Override
    protected void deleteAll(List<String> annotationUris) throws RepositoryException, UpdateExecutionException {
    	    	
    	for(String uri : annotationUris) {   		
    		String removeIncomingsAnnotationQuery = getRemoveAllSuperAnnotationQuery(uri).buildRequest().toString(); 
    		String removeAnnotationQuery = getRemoveAllAnnotationTripleQuery(uri).buildRequest().toString(); 
    		
    		Update update = connection.prepareUpdate(QueryLanguage.SPARQL,removeIncomingsAnnotationQuery); 
    		update.execute(); // first delete all annotation which has the annotationUri as target  
    		update = connection.prepareUpdate(QueryLanguage.SPARQL,removeAnnotationQuery); 
    		update.execute(); // then delete the annotation itself
    	}	
    }
   
    @Override
    public void delete(List<Annotation> annotations) throws DAOPersistenceException, Exception, IllegalAccessException, IllegalAccessException { 
    	
    	ArrayList<String> uris = annotations.stream().map(event -> event.getUri()) 
				.collect(Collectors.toCollection(ArrayList::new));
    	checkAndDeleteAll(uris);
    }
    
    @Override
    public List<Annotation> update(List<Annotation> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Annotation find(Annotation object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Annotation findById(String id) throws DAOPersistenceException, Exception {
        try {
            List<Annotation> annotations = find(id, null, null, null, null, true, 0, 1);
            if(!annotations.isEmpty()) {
                return annotations.get(0);
            }
        } catch (RepositoryException|MalformedQueryException|QueryEvaluationException ex) {
            handleTriplestoreException(ex);
        }
        return null;
    }
}
