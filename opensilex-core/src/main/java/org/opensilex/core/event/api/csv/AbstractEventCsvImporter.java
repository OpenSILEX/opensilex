package org.opensilex.core.event.api.csv;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.csv.CSVCell;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.time.InstantModel;
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    protected AbstractEventCsvImporter(SPARQLService sparql, OntologyDAO ontologyDAO, InputStream file, AccountModel user) throws SPARQLInvalidClassDefinitionException, SPARQLMapperNotFoundException {
        this.ontologyDAO = ontologyDAO;
        this.file = file;
        this.user = user;
        validation = new CSVValidationModel();

        managedProperties = sparql.getMapperIndex().getForClass(EventModel.class)
                .getClassAnalyzer()
                .getManagedProperties().stream().map(property -> URIDeserializer.formatURIAsStr(property.getURI()))
                .collect(Collectors.toSet());
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

            String[] csvHeader = readHeader(csvReader);

            List<URI> customProperties = null;

            // read custom header if additional columns are found
            if(!validation.hasErrors() && csvHeader.length > getHeader().size()){
                customProperties = readCustomHeader(csvHeader);
            }

            // skip the header description row
            csvReader.parseNext();

            if(! validation.hasErrors()){
                readAndValidateBody(csvReader, customProperties);
            }

        }catch (IOException | URISyntaxException | ParseException e){
            throw e;
        }
    }

    protected List<URI> readCustomHeader(String[] header) throws Exception {

        URI eventTypeURI = new URI(Oeev.Event.getURI());

        int propsBeginIndex = getHeader().size();
        List<URI> customProperties = new ArrayList<>();

        // read each custom column and check if a property match into the ontology
        for (int i = propsBeginIndex; i < header.length; i++) {
            String property = header[i];

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

    protected String[] readHeader(CsvParser csvReader) throws Exception {

        String[] csvHeader = csvReader.parseNext();
        List<String> missingHeaders = new LinkedList<>();

        if(csvHeader == null || csvHeader.length == 0){
            validation.addMissingHeaders(getHeader());
            return csvHeader;
        }

        Iterator<String> expectedHeaderIt = getHeader().iterator();
        int i = 0;

        // ensure that the expected header match with the begin of the csv header
        while (expectedHeaderIt.hasNext()){
            String expectedColumn = expectedHeaderIt.next();

            if(i < csvHeader.length){
                String csvColumn = csvHeader[i];

                if(! expectedColumn.equalsIgnoreCase(csvColumn)){
                    validation.addInvalidHeaderURI(i,csvColumn);
                }
                i++;
            }
            else{
                missingHeaders.add(expectedColumn);
            }
        }

        if(! missingHeaders.isEmpty()){
            validation.addMissingHeaders(missingHeaders);
        }

        return csvHeader;
    }

    private void readAndValidateBody(CsvParser csvReader, List<URI> customProperties) throws Exception {

        String[] row;

        // start at the 2nd row after two header rows
        int rowIndex = 2;
        models = new ArrayList<>();

        Map<URI, ClassModel> classesByType = new HashMap<>();
        Map<URI, List<URI>> missedPropertiesByType = new HashMap<>();

        while((row = csvReader.parseNext()) != null){
            T model = getNewModel();

            AtomicInteger colIndex = new AtomicInteger(0);
            readAndValidateRow(model,row,rowIndex,colIndex,customProperties,classesByType,missedPropertiesByType);
            models.add(model);
            rowIndex++;
            colIndex.set(0);
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
                                   AtomicInteger colIndex,
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
                CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), type.toString(), SPARQLResourceModel.TYPE_FIELD);
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
                    CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), propValue, "unknown property " + property.toString() + " for type " + model.getType());
                    validation.addInvalidValueError(csvCell);
                }

            } else {
                if(nullOrEmpty){
                    // required property with a null/empty value -> error
                    if(propertyRestriction.isRequired()){
                        CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), propValue, property.toString());
                        validation.addMissingRequiredValue(csvCell);
                    }
                }else {
                    // invalid value -> error
                    if(! ontologyDAO.validateThenAddObjectRelationValue(null, classModel, property, propValue, model)){
                        CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), propValue, property.toString());
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
                CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), null, property.toString());
                validation.addMissingRequiredValue(csvCell);
            });
        }

    }

    protected void readAndValidateRow(T model , String[] row, int rowIndex, AtomicInteger colIndex,
                                      List<URI> customProperties,
                                      Map<URI,ClassModel> classesByTypeIndex,
                                      Map<URI,List<URI>> missedPropertiesByType
    ) throws Exception {
        readCommonsProps(model,row,rowIndex,colIndex);
        readCustomProps(model,row,rowIndex,colIndex,customProperties,classesByTypeIndex,missedPropertiesByType);
    }

    /**
     *
     * @param model a new model
     * @param row the list of column value inside a row
     * @param rowIndex the row index into the file
     * @param colIndex the current col index into the row
     */
    protected void readCommonsProps(T model , String[] row, int rowIndex, AtomicInteger colIndex) throws URISyntaxException{

        if(row.length < getHeader().size()){

            // append each missed column as invalid value
            getHeader().stream().skip(row.length).forEach(header -> {
                CSVCell cell = new CSVCell(rowIndex,colIndex.get(), "No value for column",header);
                validation.addInvalidValueError(cell);
            });
            return;
        }

        String uri = row[colIndex.getAndIncrement()];
        if(!StringUtils.isEmpty(uri)){
            model.setUri(new URI(uri));
        }

        String type = row[colIndex.getAndIncrement()];
        if(!StringUtils.isEmpty(type)){
            model.setType(new URI(type));
        }

        String isInstant = row[colIndex.getAndIncrement()];
        if (StringUtils.isEmpty(isInstant)) {
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, isInstant,"isInstant");
            validation.addMissingRequiredValue(cell);
        }else{
            model.setIsInstant(Boolean.parseBoolean(isInstant));
        }

        String start = row[colIndex.getAndIncrement()];
        if(! StringUtils.isEmpty(start)) {
            InstantModel startModel = new InstantModel();
            try{
                startModel.setDateTimeStamp(OffsetDateTime.parse(start));
                model.setStart(startModel);
            }catch (DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, start,"start");
                validation.addInvalidValueError(cell);
            }
        }

        String end = row[colIndex.getAndIncrement()];
        if (StringUtils.isEmpty(end)) {

            if(model.getIsInstant() == null || model.getIsInstant()){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, end,"end");
                validation.addMissingRequiredValue(cell);
            }
            else if (StringUtils.isEmpty(start)) {
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-2, start,"start");
                validation.addMissingRequiredValue(cell);
            } 
        } else {
            InstantModel endModel = new InstantModel();
            try {
                endModel.setDateTimeStamp(OffsetDateTime.parse(end));
                model.setEnd(endModel);
            }catch (DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, end,"end");
                validation.addInvalidValueError(cell);
            }
        }

        // End & start comparison
        if (model.getStart() != null && model.getEnd() != null) {
            if (model.getStart().getDateTimeStamp().isAfter(model.getEnd().getDateTimeStamp())) {
                CSVCell cell = new CSVCell(rowIndex,colIndex.get(), start, EventModel.START_FIELD);
                cell.setMessage("EventCsvForm.invalidDate");
                validation.addInvalidDateErrors(cell);
            }
        }

        String target = row[colIndex.getAndIncrement()];
        if(StringUtils.isEmpty(target)){
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, target,"Target");
            validation.addMissingRequiredValue(cell);
        }else{
            URI targetUri = new URI(target);
            model.setTargets(Collections.singletonList(targetUri));
        }

        String description = row[colIndex.getAndIncrement()];
        model.setDescription(description);

    }
}
