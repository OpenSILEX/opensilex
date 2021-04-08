package org.opensilex.core.event.api.csv;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.ontology.dal.CSVCell;
import org.opensilex.core.ontology.dal.CSVValidationModel;
import org.opensilex.sparql.model.time.InstantModel;
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

    private final static LinkedHashSet<String> header = new LinkedHashSet<>(Arrays.asList(
            "URI", "Type", "IsInstant", "Start", "End", "Target", "Description"
    ));

    Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final InputStream file;
    protected final CSVValidationModel validation;
    private List<T> models;
    protected final URI creator;


    public AbstractEventCsvImporter(InputStream file, URI creator){
        this.file = file;
        this.creator = creator;
        validation = new CSVValidationModel();
    }

    public List<T> getModels() {
        return models;
    }

    public CSVValidationModel getValidation() {
        return validation;
    }

    protected  LinkedHashSet<String> getHeader(){
        return header;
    }

    public void readFile(boolean validateOnly) throws ParseException, IOException, URISyntaxException {

        try (Reader inputReader = new InputStreamReader(file, StandardCharsets.UTF_8.name())) {
            CsvParserSettings csvParserSettings = ClassUtils.getCSVParserDefaultSettings();
            CsvParser csvReader = new CsvParser(csvParserSettings);
            csvReader.beginParsing(inputReader);
            LOGGER.debug("Import events - CSV format => \n '" + csvReader.getDetectedFormat()+ "'");

            readAndValidateHeader(csvReader);

            if(! validation.hasErrors()){
                readAndValidateBody(csvReader,validateOnly);
            }

        }catch (IOException | URISyntaxException | ParseException e){
            throw e;
        }
    }

    protected void readAndValidateHeader(CsvParser csvReader) throws IOException {

        String[] csvFileHeader = csvReader.parseNext();
        List<String> missingHeaders = new LinkedList<>();

        if(csvFileHeader == null || csvFileHeader.length == 0){
            validation.addMissingHeaders(header);
            return;
        }

        // ensure header is equals to the expected header
        Iterator<String> headerIt = getHeader().iterator();
        int i = 0;

        while (headerIt.hasNext()){
            String expectedHeader = headerIt.next();

            if(i < csvFileHeader.length){
                String fileHeader = csvFileHeader[i];
                if(! expectedHeader.equals(fileHeader)){
                    validation.addInvalidHeaderURI(i,fileHeader);
                }
                i++;
            }
            else{
                missingHeaders.add(expectedHeader);
            }
        }

        if(! missingHeaders.isEmpty()){
            validation.addMissingHeaders(missingHeaders);
        }

        // skip the header description row
        csvReader.parseNext();
    }

    private void readAndValidateBody(CsvParser csvReader,boolean validateOnly) throws IOException, URISyntaxException, ParseException {

        String[] row;

        // start at the 2nd row after two header rows
        int rowIndex = 2;
        models = new ArrayList<>();


        while((row = csvReader.parseNext()) != null){
            T model = getNewModel();
            readAndValidateRow(model,row,rowIndex, new AtomicInteger(0));
            if(! validateOnly){
                models.add(model);
            }
            rowIndex++;
        }

        if(rowIndex == 2){
            CSVCell csvCell = new CSVCell(rowIndex, 0, null, "Empty row");
            validation.addMissingRequiredValue(csvCell);
        }
    }

    protected abstract T getNewModel();

    /**
     *
     * @param model a new model
     * @param row the list of column value inside a row
     * @param rowIndex the row index into the file
     * @param colIndex the current col index into the row
     */
    protected void readAndValidateRow(T model , String[] row, int rowIndex, AtomicInteger colIndex) throws URISyntaxException{

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

            if(model.getIsInstant()){
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

                if(model.getIsInstant()){
                    model.setStart(model.getEnd());
                }
            }catch (DateTimeParseException e){
                CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, end,"end");
                validation.addInvalidValueError(cell);
            }

        }

        String concernedItem = row[colIndex.getAndIncrement()];
        URI concernedItemUri = null;

        if(StringUtils.isEmpty(concernedItem )){
            CSVCell cell = new CSVCell(rowIndex,colIndex.get()-1, concernedItem,"Target");
            validation.addMissingRequiredValue(cell);
        }else{
            concernedItemUri = new URI(concernedItem);
            model.setConcernedItems(Collections.singletonList(concernedItemUri));
        }

        String description = row[colIndex.getAndIncrement()];
        model.setDescription(description);
        model.setCreator(creator);

    }
}
