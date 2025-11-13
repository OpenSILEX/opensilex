package org.opensilex.sparql.csv;

import org.opensilex.sparql.model.SPARQLResourceModel;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Interface which define basic operations when dealing with CSV file import
 * @param <T> SPARQLResourceModel type
 * @author rcolin
 */
public interface CsvImporter<T extends SPARQLResourceModel> {

    /**
     * Performs a CSV file import by handling validation + objects creation in databases
     * @param file CSV file (not-null)
     * @param validOnly indicate if only validation must be performed
     * @param modelsConsumer an optional consumer on parsed models. Can be useful in order to collect or apply custom operations after
     * batch or full file read.
     * @return the {@link CSVValidationModel} which contains information about validation status, errors and objects/metadata written during import
     *
     * @apiNote
     * <ul>
     *      <li>It's the responsibility of implementation to define in which way the file is read (by row or column), if the file import is batch or not </li>
     *      <li>Transaction lifecycle (START, COMMIT, ROLLBACK) are not intended to be directly handled by the importer.
     *           <ul>
     *               <li>For validation errors, which are detected via the throw of a particular Exception (ex : {@link java.net.URISyntaxException} or {@link org.opensilex.sparql.exceptions.SPARQLException},
     *               the importer should handle these exception by updating the {@link CSVValidationModel}. Only unexpected exception should be rethrown.
     *               </li>
     *               <li>You can defined a custom implementation which handle these transactions</li>
     *               <li>You can handle it out of csv import by just performing import inside a transactional handling block</li>
     *           </ul>
     *      </li>
     * </ul>
     */
    CSVValidationModel importCSV(File file, boolean validOnly, BiConsumer<CSVValidationModel, Stream<T>> modelsConsumer) throws Exception;

    /**
     * Performs a CSV file import by handling validation + objects creation in databases
     * @param file CSV file (not-null)
     * @param validOnly indicate if only validation must be performed
     * @return the {@link CSVValidationModel} which contains information about validation status, errors and objects/metadata written during import
     */
    default CSVValidationModel importCSV(File file, boolean validOnly) throws Exception {
        return importCSV(file,validOnly,null);
    }

    /**
     * Import {@code models} parsed and validated
     * @param validation current state of validation
     * @param models models to be inserted
     * @param modelChunkToUpdate models to be updated
     * @throws Exception if an unexpected errors occurs during models insertion in database
     *
     * @apiNote {@link #importCSV(File, boolean, BiConsumer)} and {@link #importCSV(File, boolean)} should
     * rely on this method for the insertion of models which are validated. It allows implementation
     * to define a custom way to insert and update models, without affecting the validation and parsing logic.
     */
    void upsert(CSVValidationModel validation, List<T> models, List<T> modelChunkToUpdate) throws Exception;

}
