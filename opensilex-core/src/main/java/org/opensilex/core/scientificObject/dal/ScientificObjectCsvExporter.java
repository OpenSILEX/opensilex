package org.opensilex.core.scientificObject.dal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.jena.vocabulary.RDFS;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.csv.export.AbstractCsvExporter;
import org.opensilex.sparql.csv.export.CsvExportOption;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScientificObjectCsvExporter extends AbstractCsvExporter<ScientificObjectModel> {

    private final CsvExportOption<ScientificObjectModel> exportOptions;

    public ScientificObjectCsvExporter(
            SPARQLService sparql,
            List<ScientificObjectModel> objects,
            URI experiment,
            String lang,
            Map<URI, URI> arrivalFacilityByOs,
            Map<String, Geometry> geospatialMap

    ) {
        super(sparql);

        Set<String> customColumns = new LinkedHashSet<>();
        customColumns.add(RDFS.label.toString());

        if (experiment != null) {
            customColumns.add(Oeso.isPartOf.toString());
            customColumns.add(Oeso.hasCreationDate.toString());
            customColumns.add(Oeso.hasDestructionDate.toString());
            customColumns.add(Oeso.hasFactorLevel.toString());
            customColumns.add(Oeso.isHosted.toString());
            customColumns.add(Oeso.hasReplication.toString());
            customColumns.add(Oeso.isHosted.toString());
            customColumns.add(Oeso.isPartOf.toString());
        }
        customColumns.add(Oeso.hasGeometry.toString());

        exportOptions = new CsvExportOption<ScientificObjectModel>()
                .setClassURI(URI.create(Oeso.ScientificObject.getURI()))
                .setObjectClass(ScientificObjectModel.class)
                .setLang(lang)
                .setColumns(customColumns)
                .setResults(objects);

        customRelationWriteRegistration(arrivalFacilityByOs);
        factorLevelWriteRegistration();
        geometryWriteRegistration(geospatialMap);
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

    private void  geometryWriteRegistration(Map<String, Geometry> geospatialMap){

        // oeso:geometry -> write Geometry object as String
        customRelationWrite(Oeso.hasGeometry.getURI(), object -> {
            String uriStr = object.getUri().toString();
            if (geospatialMap.containsKey(uriStr)) {
                try {
                    return GeospatialDAO.geometryToWkt(geospatialMap.get(uriStr));
                } catch (JsonProcessingException | ParseException e) {
                    throw new RuntimeException(e);
                }
            } else {
                return null;
            }
        });
    }

    private void customRelationWriteRegistration(Map<URI, URI> arrivalFacilityByOs) {

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

        // oeso:isHosted -> use corresponding facility according last move which concerns the object
        customRelationWrite(Oeso.isHosted.getURI(), object ->
                arrivalFacilityByOs.containsKey(object.getUri()) ? arrivalFacilityByOs.get(object.getUri()).toString() : null
        );
    }


    public CsvExportOption<ScientificObjectModel> getExportOptions() {
        return exportOptions;
    }
}
