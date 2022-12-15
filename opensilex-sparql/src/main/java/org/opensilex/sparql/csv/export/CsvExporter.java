package org.opensilex.sparql.csv.export;


import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;

import java.io.IOException;
import java.util.function.Function;

/**
 * @author rcolin
 * Class used for exporting any {@link SPARQLResourceModel} inside a CSV format
 * @param <T> SPARQLResourceModel type */
public interface CsvExporter<T extends SPARQLResourceModel> {

    /**
     *
     * @return CSV content
     * @throws IOException if CSV content write fail
     * @throws SPARQLException if some error occurs during a SPARQL query evaluated during export
     */
    byte[] exportCSV() throws IOException, SPARQLException;

    /**
     *
     * @return the {@link CsvExportOption} used for this export.
     * The importer rely on several options in order to determine how to export the file.
     * @apiNote
     * <pre>
     *     - {@link CsvExportOption#getColumns()} : the Set of column to export.
     *          - (use {@link org.opensilex.sparql.ontology.store.OntologyStore} if not specified, to determine which columns must be exported
     *     - {@link CsvExportOption#getClassURI()} : URI of the root class for which instance are exported
     *     - {@link CsvExportOption#getResults()} : List of models to export
     *     - {@link CsvExportOption#getLang()} : Export lang, used for header description translation
     * </pre>
     */
    CsvExportOption<T> getExportOptions();

    /**
     * Set a custom way to handle some column
     * @param columnURI URI of the column
     * @param objectRelationWriter Function which return the value to write, for some object relation
     */
    void customRelationWrite(String columnURI, Function<T,String> objectRelationWriter);

}
