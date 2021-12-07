package org.opensilex.core.event.api.csv;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.csv.dal.CSVCell;
import org.opensilex.core.csv.dal.error.CSVValidationModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.security.authentication.NotFoundURIException;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.OwlRestrictionModel;
import org.opensilex.sparql.ontology.dal.PropertyModel;
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

public abstract class AbstractEventCsvImporter<T extends EventModel> {

    public final static String TARGET_COLUMN = "targets";

    protected final static LinkedHashSet<String> EVENT_HEADER = new LinkedHashSet<>(Arrays.asList(
            EventModel.URI_FIELD,
            EventModel.TYPE_FIELD,
            EventModel.IS_INSTANT_FIELD,
            EventModel.START_FIELD,
            EventModel.END_FIELD,
            TARGET_COLUMN,
            EventModel.DESCRIPTION_FIELD
    ));

    public final static int ROWS_BEGIN_IDX = 2;

    Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final InputStream file;
    private List<T> models;

    protected final CSVValidationModel validation;
    protected final OntologyDAO ontologyDAO;

    private final UserModel user;

    public AbstractEventCsvImporter(OntologyDAO ontologyDAO, InputStream file, UserModel user) {
        this.ontologyDAO = ontologyDAO;
        this.file = file;
        this.user = user;
        validation = new CSVValidationModel();
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

    public void readFile(boolean validateOnly) throws Exception {

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import events - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            String[] csvHeader = readHeader(csvReader);

            LinkedHashSet<URI> customProperties = null;

            // read custom header if additional columns are found
            if(!validation.hasErrors() && csvHeader.length > getHeader().size()){
                customProperties = readCustomHeader(csvHeader);
            }

            // skip the header description row
            csvReader.parseNext();

            if(! validation.hasErrors()){
                readAndValidateBody(csvReader,validateOnly, customProperties);
            }

        }catch (IOException | URISyntaxException | ParseException e){
            throw e;
        }
    }

    protected LinkedHashSet<URI> readCustomHeader(String[] header) throws Exception {

        URI eventUri = new URI(Oeev.Event.getURI());

        int propsBeginIndex = getHeader().size();
        LinkedHashSet<URI> customProperties = new LinkedHashSet<>();

        // read each custom column and check if a property match into the ontology
        for (int i = propsBeginIndex; i < header.length; i++) {
            String property = header[i];

            if(StringUtils.isEmpty(property)){
                validation.addInvalidHeaderURI(i,property);
            }else{
                URI propertyUri = new URI(property);

                // searching for a data-type or object-type property
                PropertyModel propertyModel = ontologyDAO.getDataProperty(propertyUri,eventUri,this.user.getLanguage());
                if(propertyModel == null){
                    propertyModel = ontologyDAO.getObjectProperty(propertyUri,eventUri,this.user.getLanguage());
                    if(propertyModel == null){
                        CSVCell csvCell = new CSVCell(0, i, null, property);
                        validation.addInvalidURIError(csvCell);
                    }
                }

                if(propertyModel != null){
                    customProperties.add(propertyUri);
                }
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

    private void readAndValidateBody(CsvParser csvReader,boolean validateOnly, LinkedHashSet<URI> customProperties) throws Exception {

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
            if(! validateOnly){
                models.add(model);
            }
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
                                   LinkedHashSet<URI> customProperties,
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
                CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), type.toString(), EventModel.TYPE_FIELD);
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
            OwlRestrictionModel propertyRestriction = classModel.getRestrictions().get(property);

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
                    if(! ontologyDAO.validateObjectValue(null, classModel, property, propValue, model)){
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

            for (Map.Entry<URI, OwlRestrictionModel> entry : classModel.getRestrictions().entrySet()) {
                if (entry.getValue().isRequired() && !customProperties.contains(entry.getKey())) {
                    missedRequiredProperties.add(entry.getKey());
                }
            }

            missedPropertiesByType.put(model.getType(),missedRequiredProperties);
        }


        if(! missedRequiredProperties.isEmpty()){
            missedRequiredProperties.forEach(property -> {
                CSVCell csvCell = new CSVCell(rowIndex, colIndex.get(), null, property.toString());
                validation.addMissingRequiredValue(csvCell);
            });
        }

    }

    protected void readAndValidateRow(T model , String[] row, int rowIndex, AtomicInteger colIndex,
                                      LinkedHashSet<URI> customProperties,
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

        }else{
            InstantModel endModel = new InstantModel();
            try {
                endModel.setDateTimeStamp(OffsetDateTime.parse(end));
                model.setEnd(endModel);
            }catch (DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, end,"end");
                validation.addInvalidValueError(cell);
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
        model.setCreator(user.getUri());

    }
}
