package org.opensilex.core.event.api.csv;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.bll.EventLogic;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.event.dal.EventSearchFilter;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.dal.PropertyModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractEventCsvImporter<T extends EventModel> {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventCsvImporter.class);

    public static final String TARGET_COLUMN = "targets";

    protected static final LinkedHashSet<String> EVENT_HEADER = new LinkedHashSet<>(Arrays.asList(
            SPARQLResourceModel.URI_FIELD,
            SPARQLResourceModel.TYPE_FIELD,
            EventModel.IS_INSTANT_FIELD,
            EventModel.START_FIELD,
            EventModel.END_FIELD,
            TARGET_COLUMN,
            EventModel.DESCRIPTION_FIELD
    ));

    /**
     * The {@link Set} of properties URI, which are manually handled for an {@link EventModel}
     */
    protected final Set<String> managedProperties;

    public static final int ROWS_BEGIN_IDX = 2;


    private final InputStream file;
    private List<T> models;

    protected final CSVValidationModel validation;
    protected final OntologyDAO ontologyDAO;

    private final AccountModel user;
    private final EventLogic<EventModel, EventSearchFilter> eventLogic;

    protected AbstractEventCsvImporter(SPARQLService sparql, OntologyDAO ontologyDAO, InputStream file, AccountModel user, MongoDBService mongo) throws SPARQLException, SPARQLDeserializerNotFoundException {
        this.ontologyDAO = ontologyDAO;
        this.file = file;
        this.user = user;
        validation = new CSVValidationModel();

        managedProperties = sparql.getMapperIndex().getForClass(EventModel.class)
                .getClassAnalyzer()
                .getManagedProperties().stream().map(property -> URIDeserializer.formatURIAsStr(property.getURI()))
                .collect(Collectors.toSet());

        eventLogic = new EventLogic<>(sparql, mongo, user, EventModel.class);
    }


    public List<T> getModels() {
        return models;
    }

    public CSVValidationModel getValidation() {
        return validation;
    }

    protected  LinkedHashSet<String> getHeader(){
        return EVENT_HEADER;
    }

    public void readFile() throws Exception {

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import events - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            //Fetch all headers by index and perform checks on the common props
            Map<Integer, String> headerByColIndex = readHeader(csvReader);
            //Leave immediately if read header is empty or null (as no other operations performed if there are errors)
            if(MapUtils.isEmpty(headerByColIndex)){
                return;
            }

            List<URI> customProperties = null;

            // read custom header if additional columns are found
            if(!validation.hasErrors() && headerByColIndex.size() > getHeader().size()){
                customProperties = readCustomHeader(headerByColIndex);
            }

            // skip the header description row
            csvReader.parseNext();

            if(! validation.hasErrors()){
                readAndValidateBody(csvReader, customProperties, headerByColIndex);
            }

        }catch (IOException | URISyntaxException | ParseException e){
            throw e;
        }
    }

    protected List<URI> readCustomHeader(Map<Integer, String> headerByIndex) throws Exception {

        URI eventTypeURI = new URI(Oeev.Event.getURI());

        int propsBeginIndex = getHeader().size();
        List<URI> customProperties = new ArrayList<>();

        // read each custom column and check if a property match into the ontology
        for (int i = propsBeginIndex; i < headerByIndex.size(); i++) {
            String property = headerByIndex.get(i);

            if(StringUtils.isEmpty(property)){
                validation.addInvalidHeaderURI(i,property);
                continue;
            }

            try{
                // ensure URI is well formatted, else an error will be encountered during property retrieval
                URI propertyUri = URIDeserializer.formatURI(new URI(property));
                if(! propertyUri.isAbsolute()){
                    validation.addInvalidHeaderURI(i,property);
                    continue;
                }

                // searching for a data-type or object-type property
                PropertyModel propertyModel = ontologyDAO.getDataProperty(propertyUri,eventTypeURI,this.user.getLanguage());
                if(propertyModel == null){
                    propertyModel = ontologyDAO.getObjectProperty(propertyUri,eventTypeURI,this.user.getLanguage());
                    if(propertyModel == null){
                        validation.addInvalidHeaderURI(i,property);
                    }
                }

                if(propertyModel != null){
                    customProperties.add(propertyUri);
                }
            }catch (URISyntaxException e){
                // error on URI parsing
                validation.addInvalidHeaderURI(i,property);
            }

        }

        return customProperties;
    }

    /**
     * Reads the entire header line then performs validations on the Common properties
     * (Missing headers, duplicated headers and Invalid headers)
     *
     * @param csvReader contains the information of CSV we are reading
     * @return A map of colIndex -> prop name, so that any order will be accepted for common props
     */
    protected Map<Integer, String> readHeader(CsvParser csvReader) throws Exception {

        String[] csvHeaderRowValues = csvReader.parseNext();

        if(csvHeaderRowValues == null || csvHeaderRowValues.length == 0){
            validation.addMissingHeaders(getHeader());
            return null;
        }

        //Place expected headers in a constant
        final LinkedHashSet<String> expectedHeaders = getHeader();

        //Prepare a list of visited headers, at the end if size is smaller than expected, we'll be able to identify which ones
        List<String> visitedExpectedHeaders = new ArrayList<>();

        //Prepare return value
        Map<Integer, String> headersByIndex = new HashMap<>();

        //Iterate over read headers, if we are below the expected columns size then perform checks,
        // else simply add to result
        for(int i = 0; i < csvHeaderRowValues.length; i++){
            String nextHeader = csvHeaderRowValues[i];
            //If we are in the custom props zone then simply add to result and continue
            if(i >= expectedHeaders.size()){
                headersByIndex.put(i, nextHeader);
                continue;
            }
            //Verify was not already visited
            if(visitedExpectedHeaders.contains(nextHeader)){
                validation.addInvalidDuplicateHeader(i, nextHeader);
                continue;
            }
            //Is expected check
            if(!expectedHeaders.contains(nextHeader)){
                validation.addInvalidHeaderURI(i, nextHeader);
                continue;
            }
            //Else not already visited and is an expected column
            visitedExpectedHeaders.add(nextHeader);
            headersByIndex.put(i, nextHeader);
        }

        //Now that we have finished iterating we can verify that no columns missing
        if(visitedExpectedHeaders.size() < expectedHeaders.size()){
            //Filter out visited headers from expected and set this as missing headers
            List<String> missingHeaders = expectedHeaders.stream().filter(expected -> !visitedExpectedHeaders.contains(expected)).toList();
            validation.addMissingHeaders(missingHeaders);
        }

        return headersByIndex;
    }

    private void readAndValidateBody(
            CsvParser csvReader,
            List<URI> customProperties,
            Map<Integer, String> headerByColIndex
    ) throws Exception {

        String[] row;

        // start at the 2nd row after two header rows
        int rowIndex = 2;
        models = new ArrayList<>();

        Map<URI, ClassModel> classesByType = new HashMap<>();
        Map<URI, List<URI>> missedPropertiesByType = new HashMap<>();

        while((row = csvReader.parseNext()) != null){
            T model = getNewModel();

            readAndValidateRow(
                    model,
                    row,
                    rowIndex,
                    customProperties,
                    classesByType,
                    missedPropertiesByType,
                    headerByColIndex
            );
            models.add(model);
            rowIndex++;
        }

        if(rowIndex == 2){
            CSVCell csvCell = new CSVCell(rowIndex, 0, null, "Empty row");
            validation.addMissingRequiredValue(csvCell);
        }
    }

    protected abstract T getNewModel();


    protected void readCustomProps(T model,
                                   String[] row,
                                   int rowIndex,
                                   List<URI> customProperties,
                                   Map<URI, ClassModel> classesByType,
                                   Map<URI, List<URI>> missedPropertiesByType
    ) throws Exception {

        // get or compute the ClassModel associated with the model
        URI type = model.getType();
        if(type == null){
            return;
        }

        ClassModel classModel = classesByType.get(type);
        if(classModel == null){
            try{
                classModel = ontologyDAO.getClassModel(model.getType(),new URI(Oeev.Event.getURI()),user.getLanguage());
                classesByType.put(type,classModel);

            }catch (NotFoundURIException e){
                CSVCell csvCell = new CSVCell(rowIndex, getHeader().size(), type.toString(), SPARQLResourceModel.TYPE_FIELD);
                validation.addInvalidURIError(csvCell);
                return;
            }

        }

        if(CollectionUtils.isEmpty(customProperties)){
            return;
        }

        // for each column, check if a property match with the model type

        Iterator<URI> propsIt = customProperties.iterator();


        for (int i = row.length - customProperties.size(); i < row.length; i++) {

            String propValue = row[i];
            URI property = propsIt.next();

            // exclude read managed properties, already handled
            if(managedProperties.contains(property.toString())){
                continue;
            }

            OwlRestrictionModel propertyRestriction = classModel.getRestrictionsByProperties().get(property);

            boolean nullOrEmpty = StringUtils.isEmpty(propValue);

            if (propertyRestriction == null) {

                // nom-empty value and unknown property -> error
                if (!nullOrEmpty) {
                    CSVCell csvCell = new CSVCell(rowIndex, i, propValue, "unknown property " + property.toString() + " for type " + model.getType());
                    validation.addInvalidValueError(csvCell);
                }

            } else {
                if(nullOrEmpty){
                    // required property with a null/empty value -> error
                    if(propertyRestriction.isRequired()){
                        CSVCell csvCell = new CSVCell(rowIndex, i, propValue, property.toString());
                        validation.addMissingRequiredValue(csvCell);
                    }
                }else {
                    // invalid value -> error
                    if(! ontologyDAO.validateThenAddObjectRelationValue(null, classModel, property, propValue, model)){
                        CSVCell csvCell = new CSVCell(rowIndex, i, propValue, property.toString());
                        validation.addInvalidValueError(csvCell);
                    }
                }
            }

        }

        List<URI> missedRequiredProperties = missedPropertiesByType.get(model.getType());
        if(missedRequiredProperties == null){

            // compute the list of required property excluded from the custom properties list
            missedRequiredProperties = new LinkedList<>();

            for (Map.Entry<URI, OwlRestrictionModel> entry : classModel.getRestrictionsByProperties().entrySet()) {

                URI property = entry.getKey();

                // exclude read managed properties, already handled
                if (managedProperties.contains(property.toString())) {
                    continue;
                }

                // required property
                if (entry.getValue().isRequired() && !customProperties.contains(property)) {
                    missedRequiredProperties.add(property);
                }
            }

            missedPropertiesByType.put(model.getType(),missedRequiredProperties);
        }


        // if some required restriction -> error
        if(! missedRequiredProperties.isEmpty()){
            missedRequiredProperties.forEach(property -> {
                CSVCell csvCell = new CSVCell(rowIndex, getHeader().size(), null, property.toString());
                validation.addMissingRequiredValue(csvCell);
            });
        }

    }

    protected void readAndValidateRow(
            T model,
            String[] row,
            int rowIndex,
            List<URI> customProperties,
            Map<URI,ClassModel> classesByTypeIndex,
            Map<URI,List<URI>> missedPropertiesByType,
            Map<Integer, String> headerByColIndex
    ) throws Exception {
        readCommonsProps(model, row, rowIndex, headerByColIndex);
        readCustomProps(model,row,rowIndex,customProperties,classesByTypeIndex,missedPropertiesByType);
    }

    /**
     *
     * @param header we want to check if is required
     * @return true if values for this header must be filled in CSV, false if nay
     *
     * WARNING, does not cover Start and End fields as they are only required under certain conditions
     */
    private boolean isRequiredColumnValue(String header){
        return header.equals(EventModel.IS_INSTANT_FIELD) || header.equals(TARGET_COLUMN);
    }

    protected int getIndexForColumn(String column, Map<Integer, String> headerByIndex) throws Exception{
        if(!headerByIndex.containsValue(column)){
            //Throw error so we don't try to perform imposible operations
            throw new Exception("column " + column + " not found");
        }
        for(int i = 0; i < headerByIndex.size(); i++){
            String nextCol =  headerByIndex.get(i);
            if(nextCol.equals(column)){
                return i;
            }
        }
        return -1;
    }

    /**
     *
     * @param model a new model
     * @param row the list of column value inside a row
     * @param rowIndex the row index into the file
     * @param headerByColIndex map to know which column we are reading in function of where we are in row
     */
    protected void readCommonsProps(
            T model,
            String[] row,
            int rowIndex,
            Map<Integer, String> headerByColIndex
    ) throws Exception {

        //If row size is smaller than common header size then append some InvalidValue errors and leave
        if(!verifyAndHandleNotEnoughRequiredColumnsForRow(row, rowIndex, headerByColIndex)){
            return;
        }

        //Iterate over row's common props to read and check values
        for(int i = 0; i < getHeader().size(); i++){
            String nextValue = row[i];
            String correspondingHeader = headerByColIndex.get(i);

            tryToApplyValueOnGenericEventField(correspondingHeader, model, rowIndex, i, nextValue);
        }
        //Now that we've finished reading row we can check which of end or start is required and relook if they were passed or not
        verifyRequiredEndOrStart(model, rowIndex, headerByColIndex);
    }

    /**
     *
     * @return false if there are not enough required values present in this row, true else
     */
    protected boolean verifyAndHandleNotEnoughRequiredColumnsForRow(String[] row, int rowIndex, Map<Integer, String> headerByColIndex){
        if(row.length < getHeader().size()){
            for(int i = row.length; i< getHeader().size(); i++){
                CSVCell cell = new CSVCell(rowIndex, i, "No value for column", headerByColIndex.get(i));
                validation.addInvalidValueError(cell);
            }
            return false;
        }
        return true;
    }

    protected void verifyRequiredEndOrStart(T model, int rowIndex, Map<Integer, String> headerByColIndex) throws Exception{
        if (model.getEnd() == null) {
            if(model.getIsInstant() == null || model.getIsInstant()){
                CSVCell cell = new CSVCell(rowIndex, getIndexForColumn(EventModel.END_FIELD, headerByColIndex), "","end");
                validation.addMissingRequiredValue(cell);
            }
            else if (model.getStart() == null) {
                CSVCell cell = new CSVCell(rowIndex,getIndexForColumn(EventModel.START_FIELD, headerByColIndex), "","start");
                validation.addMissingRequiredValue(cell);
            }
        }
    }

    /**
     *
     * @param header for whom the value applies
     * @param model the model we are applying value too
     * @param rowIndex line index in CSV
     * @param colIndex column index in CSV
     * @param value the value we are applying
     * @return true if header corresponds to an Event header
     * @throws URISyntaxException
     */
    protected boolean tryToApplyValueOnGenericEventField(String header, T model, int rowIndex, int colIndex, String value) throws URISyntaxException {
        //If empty value then add to InvalidValue errors if it is a required value
        if(isRequiredColumnValue(header) && StringUtils.isEmpty(value)){
            CSVCell cell = new CSVCell(rowIndex, colIndex, "No value for column", header);
            validation.addInvalidValueError(cell);
            return true;
        }
        //Else this value is not an empty required value (could still be an empty non-required value), identify which check we need to do

        if(header.equals(SPARQLResourceModel.URI_FIELD)){
            if(!StringUtils.isEmpty(value)){
                model.setUri(new URI(value));
            }
            return true;
        }
        if(header.equals(SPARQLResourceModel.TYPE_FIELD)){
            if(!StringUtils.isEmpty(value)){
                model.setType(new URI(value));
            }
            return true;
        }
        if(header.equals(EventModel.IS_INSTANT_FIELD)){
            model.setIsInstant(Boolean.parseBoolean(value));
            return true;
        }
        if(header.equals(EventModel.START_FIELD) || header.equals(EventModel.END_FIELD)){
            //Field may be required, but we need Start, end and isInstant to verify so we do this at end of row
            try{
                if(header.equals(EventModel.START_FIELD)){
                    eventLogic.applyValueOnStartField(value, model);
                }else{
                    eventLogic.applyValueOnEndField(value, model);
                }

            } catch(BadRequestException | DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex, colIndex, value, header);
                cell.setMessage(e.getMessage());
                validation.addInvalidDateErrors(cell);
            }
            return true;
        }
        if(header.equals(EventModel.TARGETS_FIELD)){
            URI targetUri = new URI(value);
            model.setTargets(Collections.singletonList(targetUri));
            return true;
        }
        if(header.equals(EventModel.DESCRIPTION_FIELD)){
            model.setDescription(value);
            return true;
        }
        return false;
    }

}
