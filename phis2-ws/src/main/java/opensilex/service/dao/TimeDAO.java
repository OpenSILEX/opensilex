//******************************************************************************
//                               TimeDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 6 March 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.User;
import opensilex.service.ontology.Time;
import opensilex.service.ontology.Xsd;
import opensilex.service.utils.UriGenerator;
import opensilex.service.utils.date.Dates;
import opensilex.service.utils.sparql.SPARQLStringBuilder;

/**
 * Time object DAO.
 * @author Andreas Garcia <andreas.garcia@inra.fr>
 */
public class TimeDAO extends Rdf4jDAO<Time> {
    final static Logger LOGGER = LoggerFactory.getLogger(TimeDAO.class);
    
    protected static final String DATETIME_SELECT_NAME = "dateTime";
    protected static final String DATETIME_SELECT_NAME_SPARQL = "?" + DATETIME_SELECT_NAME;
    
    protected static final String DATE_RANGE_START_DATETIME_SELECT_NAME = "dateRangeStartDateTime";
    protected static final String DATE_RANGE_START_DATETIME_SELECT_NAME_SPARQL = "?" + DATE_RANGE_START_DATETIME_SELECT_NAME;
    
    protected static final String DATE_RANGE_END_DATETIME_SELECT_NAME = "dateRangeEndDateTime";
    protected static final String DATE_RANGE_END_DATETIME_SELECT_NAME_SPARQL = "?" + DATE_RANGE_END_DATETIME_SELECT_NAME;
    
    // constants used for SPARQL names in the SELECT
    private static final String XSD_DATE_TIME_STAMP_SELECT_NAME = "XSDDateTimeStamp";
    private static final String XSD_DATE_TIME_STAMP_SELECT_NAME_SPARQL = "?" + XSD_DATE_TIME_STAMP_SELECT_NAME;
    private static final String TIME_SELECT_NAME = "time";
    private static final String TIME_SELECT_NAME_SPARQL = "?" + TIME_SELECT_NAME;
    
    private static final String DATETIMESTAMP_SELECT_NAME = "dateTimeStamp";
    private static final String DATETIMESTAMP_SELECT_NAME_SPARQL = "?" + DATETIMESTAMP_SELECT_NAME;

    public TimeDAO(User user) {
        super(user);
    }
    
    /** 
     * Adds a filter to the search query comparing a SPARQL dateTimeStamp 
     * variable to a date. 
     * SPARQL dateTimeStamp dates have to be handled in a specific way as 
     * the comparison operators (<, >, etc.) aren't available for dateTimeStamp
     * objects.
     * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#OperatorMapping">
     * SparQL Operator Mapping
     * </a>
     * @param query
     * @param filterDateString
     * @param filterDateFormat
     * @param filterDateSparqlVariable SPARQL variable (?abc format)
     * @param comparisonSign e.g >, >=, <, <= 
     * @param dateTimeStampToCompareSparqlVariable the SPARQL variable 
     * (?abc format) of the dateTimeStamp to which the date has to be compared
     * @example SparQL code added to the query :
     *   BIND(xsd:dateTime(str("2017-09-10T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *   FILTER ( (?dateRangeStartDateTime <= ?dateTime) ) 
     */
    protected void filterSearchQueryWithDateTimeStampComparison( SPARQLStringBuilder query, String filterDateString, String filterDateFormat, String filterDateSparqlVariable, String comparisonSign, String dateTimeStampToCompareSparqlVariable){
        
        DateTime filterDate = Dates.stringToDateTimeWithGivenPattern(filterDateString, filterDateFormat);
        
        String filterDateStringInSparqlDateTimeStampFormat = DateTimeFormat.forPattern(DATETIMESTAMP_FORMAT_SPARQL).print(filterDate);

        query.appendToBody("\nBIND(<" + Xsd.FUNCTION_DATETIME.toString() + ">(str(\"" + filterDateStringInSparqlDateTimeStampFormat + "\")) as " + filterDateSparqlVariable + ") .");
        
        query.appendAndFilter(filterDateSparqlVariable + comparisonSign + dateTimeStampToCompareSparqlVariable);
    }

    /**
     * Appends a filter to select only the results whose datetime is included in 
     * the date range in parameter.
     * @param query
     * @param filterRangeDatesStringFormat
     * @param filterRangeStartDateString
     * @param filterRangeEndDateString
     * @param dateTimeStampToCompareSparqleVariable the SPARQL variable (?abc 
     * format) of the dateTimeStamp to compare to the range
     * @example SparQL code added to the query :
     *   BIND(xsd:dateTime(str(?dateTimeStamp)) as ?dateTime) .
     *   BIND(xsd:dateTime(str("2017-09-10T12:00:00+01:00")) as ?dateRangeStartDateTime) .
     *   BIND(xsd:dateTime(str("2017-09-12T12:00:00+01:00")) as ?dateRangeEndDateTime) .
     *   FILTER ( (?dateRangeStartDateTime <= ?dateTime) && (?dateRangeEndDateTime >= ?dateTime) ) 
     */
    protected void filterSearchQueryWithDateRangeComparisonWithDateTimeStamp(SPARQLStringBuilder query, String filterRangeDatesStringFormat, String filterRangeStartDateString, String filterRangeEndDateString, String dateTimeStampToCompareSparqleVariable){
        
        query.appendToBody("\nBIND(<" + Xsd.FUNCTION_DATETIME.toString() + ">(str(" + dateTimeStampToCompareSparqleVariable + ")) as " + DATETIME_SELECT_NAME_SPARQL + ") .");
        
        if (filterRangeStartDateString != null){
            filterSearchQueryWithDateTimeStampComparison(
                    query, 
                    filterRangeStartDateString, 
                    filterRangeDatesStringFormat, 
                    DATE_RANGE_START_DATETIME_SELECT_NAME_SPARQL, 
                    " <= ", 
                    DATETIME_SELECT_NAME_SPARQL);
        }
        if (filterRangeEndDateString != null){
            filterSearchQueryWithDateTimeStampComparison(
                    query, 
                    filterRangeEndDateString, 
                    filterRangeDatesStringFormat, 
                    DATE_RANGE_END_DATETIME_SELECT_NAME_SPARQL, 
                    " >= ", 
                    DATETIME_SELECT_NAME_SPARQL);
        }
    }
    
    /**
     * Inserts an Instant linked to the given URI in the given graph with the 
     * given date value.
     * @param updateBuilder
     * @param graph
     * @param resourceLinkedToInstant
     * @param dateTime
     * @throws java.lang.Exception
     */
    public void addInsertToUpdateBuilderWithInstant(UpdateBuilder updateBuilder, Node graph, Resource resourceLinkedToInstant, DateTime dateTime) throws Exception {
        // Add insert instant uri with type
        UriGenerator uriGenerator = new UriGenerator();
        String instantUri = uriGenerator.generateNewInstanceUri(Time.Instant.toString(), null, null);
        Resource instantResource = ResourceFactory.createResource(instantUri);
        updateBuilder.addInsert(graph, instantResource, RDF.type, Time.Instant);

        // Add date time stamp to instant
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DATETIMESTAMP_FORMAT_SPARQL);
        Literal dateLiteral = ResourceFactory.createTypedLiteral(dateTime.toString(formatter), XSDDatatype.XSDdateTime);
        updateBuilder.addInsert(graph, instantResource, Time.inXSDDateTimeStamp, dateLiteral);

        // Link resource to instant
        updateBuilder.addInsert(graph, resourceLinkedToInstant, Time.hasTime, instantResource);
    }

    @Override
    public List<Time> create(List<Time> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Time> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Time> update(List<Time> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Time find(Time object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Time findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Time> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
