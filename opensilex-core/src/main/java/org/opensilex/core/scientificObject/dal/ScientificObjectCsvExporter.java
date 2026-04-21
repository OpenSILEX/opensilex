package org.opensilex.core.scientificObject.dal;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.event.bll.MoveLogic;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.scientificObject.bll.ScientificObjectCsvImporterLogic;
import org.opensilex.core.utils.StringUriMap;
import org.opensilex.sparql.csv.export.AbstractCsvExporter;
import org.opensilex.sparql.csv.export.CsvExportOption;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.*;

public class ScientificObjectCsvExporter extends AbstractCsvExporter<ScientificObjectModel> {

    private final CsvExportOption<ScientificObjectModel> exportOptions;
    private final StringUriMap<MoveModel> initialMovePerTarget;

    public ScientificObjectCsvExporter(
            SPARQLService sparql,
            List<ScientificObjectModel> objects,
            URI experiment,
            String lang,
            MoveLogic moveLogic
    ) throws Exception {
        super(sparql);

        // get first locations of objects only if we are in an Experiment because we only export
        this.initialMovePerTarget = moveLogic.getInitialMovesWithLocationPerTarget(objects.stream().map(ScientificObjectModel::getUri).toList(), experiment);

        final Set<String> customColumns;
        final Set<String> extraNonUriColumns;

        if (experiment != null) {
            // just let the export find which properties are associated to an OS
            customColumns = Collections.emptySet();
            extraNonUriColumns = ScientificObjectCsvImporterLogic.extraColumns;
        }else{
            // only export name and geometry when out of experimental context
            customColumns = new LinkedHashSet<>();
            customColumns.add(RDFS.label.toString());
            extraNonUriColumns = Collections.emptySet();
        }

        exportOptions = new CsvExportOption<ScientificObjectModel>()
                .setClassURI(URI.create(Oeso.ScientificObject.getURI()))
                .setObjectClass(ScientificObjectModel.class)
                .setLang(lang)
                .setUriColumnsAsStrings(customColumns)
                .setExtraColumns(extraNonUriColumns)
                .setResults(objects);

        customRelationWriteRegistration();
        factorLevelWriteRegistration();
    }

    @Override
    protected void writeExtraStringColumnValue(int colIdx, String column, ScientificObjectModel object, String[] lineBuffer) throws Exception{
        //For now this function only exists to add the Move related stuff to the export, so obviously we need to go get that Move
        MoveModel correspondingMove = this.initialMovePerTarget.get(object.getUri());
        if(correspondingMove == null){
            return;
        }
        if(column.equals(ScientificObjectCsvImporterLogic.MOVE_START_FIELD_UNIQUE_HEADER)){
            if(correspondingMove.getStart() == null){
                return;
            }
            lineBuffer[colIdx] = correspondingMove.getStart().getDateTimeStamp().toLocalDate().toString();
            return;
        }
        if(column.equals(ScientificObjectCsvImporterLogic.MOVE_END_FIELD_UNIQUE_HEADER)){
            lineBuffer[colIdx] = correspondingMove.getEnd().getDateTimeStamp().toLocalDate().toString();
            return;
        }
        LocationModel movesLocation = correspondingMove.getLocationObservation().getLocation();
        if(column.equals(LocationModel.FROM_FIELD)){
            lineBuffer[colIdx] = movesLocation.getFrom() == null ? "" : SPARQLDeserializers.getShortURI(movesLocation.getFrom());
            return;
        }
        if(column.equals(LocationModel.TO_FIELD)){
            lineBuffer[colIdx] = movesLocation.getTo() == null ? "" : SPARQLDeserializers.getShortURI(movesLocation.getTo());
            return;
        }
        if(column.equals(LocationModel.GEOMETRY_FIELD)){
            lineBuffer[colIdx] = movesLocation.getGeometry() == null ? "" : GeospatialDAO.geometryToWkt(movesLocation.getGeometry());
            return;
        }
        if(column.equals(LocationModel.X_FIELD)){
            lineBuffer[colIdx] = movesLocation.getX();
            return;
        }
        if(column.equals(LocationModel.Y_FIELD)){
            lineBuffer[colIdx] = movesLocation.getY();
            return;
        }
        if(column.equals(LocationModel.Z_FIELD)){
            lineBuffer[colIdx] = movesLocation.getZ();
            return;
        }
        if(column.equals(LocationModel.TEXTUAL_POSITION_FIELD)){
            lineBuffer[colIdx] = movesLocation.getTextualPosition();
        }
    }

    private void factorLevelWriteRegistration(){

        // oeso:hasFactorLevel -> write factor levels inside one cell
        customRelationWrite(Oeso.hasFactorLevel.getURI(), object -> {
            if (object.getFactorLevels() == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (FactorLevelModel factorLevel : object.getFactorLevels()) {
                if (factorLevel != null && factorLevel.getUri() != null) {
                    sb.append(factorLevel.getUri().toString()).append(" ");
                }
            }
            // remove last space character
            return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : sb.toString();
        });
    }

    private void customRelationWriteRegistration() {

        // rdfs:label -> write object name
        customRelationWrite(RDFS.label.getURI(), SPARQLNamedResourceModel::getName);

        // oeso:isPartOf -> write parent
        customRelationWrite(Oeso.isPartOf.getURI(), object ->
                object.getParent() != null ? SPARQLDeserializers.formatURI(object.getParent().getUri()).toString() : null
        );

        // oeso:hasCreationDate LocalDate write as String
        customRelationWrite(Oeso.hasCreationDate.getURI(), object ->
                object.getCreationDate() != null ? object.getCreationDate().toString() : null
        );

        // oeso:hasDestructionDate LocalDate write as String
        customRelationWrite(Oeso.hasDestructionDate.getURI(), object ->
                object.getDestructionDate() != null ? object.getDestructionDate().toString() : null
        );

    }


    public CsvExportOption<ScientificObjectModel> getExportOptions() {
        return exportOptions;
    }
}
