package org.opensilex.sparql.csv.validation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.opensilex.sparql.csv.CSVValidationModel;
import org.opensilex.sparql.csv.CsvImporter;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.utils.TokenGenerator;

import java.io.File;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.CRC32;

/**
 * <pre>
 * A implementation of {@link CsvImporter} which can be used in order to cache validations results.
 *
 * It's particular useful in the case where validation only + import service are called in two times.
 * In this case validations results from first API call are cached using a {@link Caffeine} cache.
 * So no need to performs twice CSV parsing and SPARQL or MongoDB validation queries on the 2nd call.
 *
 * Then these results are directly written in case of import service (if token from validation is passed to import).
 *
 * @apiNote
 *
 * <pre>
 * <b>Token and file integrity </b>
 * When passing a validation token with a File, this importer ensure that the checksum of the file
 * is the same as the checksum previously computed before generating a validation token.
 * A {@link CRC32} checksum algorithm is used in order to check file integrity.
 *
 * This integrity only check for file content equality, so if the file is validated only, renamed or moved and then imported,
 * if the file content is still the same then the importer will thread this file as identical as the validated file
 *
 * <b>Limitations</b>
 * If CSV file is too big, then too much results can be stored in RAM
 * (even with a short TTL, it could lead to a big RAM usage)
 *
 * </pre>
 *
 * @param <T>
 */
public class CachedCsvImporter<T extends SPARQLResourceModel> implements CsvImporter<T> {

    /**
     * The CSV importer on which rely for creation (if validation has been previously done) or for full import (validation+creation)
     */
    private final CsvImporter<T> fallback;

    /**
     * A CSV validation token
     */
    private final String token;

    /**
     * init CSV validation cache
     * set short expiration delay and short entry length
     */
    private static final Cache<String, CSVValidationModel> validationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1000)
            .build();

    /**
     * @param fallback  the {@link CsvImporter} to use for full validation or for import only (not-null)
     * @param token a previously generated validation token. If this token match with cached validation, then only import will be performed
     * during {@link #importCSV(File, boolean, BiConsumer)} call.
     */
    public CachedCsvImporter(CsvImporter<T> fallback, String token) {
        Objects.requireNonNull(fallback);
        this.fallback = fallback;
        this.token = token;
    }

    @Override
    public CSVValidationModel importCSV(File file, boolean validOnly, BiConsumer<CSVValidationModel, Stream<T>> modelsConsumer) throws Exception {

        Objects.requireNonNull(file);

        // Compute file checksum in order to ensure file integrity
        // Use a checksum algorithm instead of non-reversible function like MD5 or SHA, which are not intended for content modification detection
        String checksum = String.valueOf(TokenGenerator.getFileChecksum(file, new CRC32()));
        CSVValidationModel validationModel = validationCache.getIfPresent(checksum);

        // new csv file
        if (validationModel == null) {

            // file read and validation computing
            validationModel = fallback.importCSV(file, validOnly, (validation, models) -> {

                // collect models if validOnly, else no need to save validation since file content is imported
                if (validOnly) {
                    models.forEachOrdered(validation.getObjects()::add);
                }

                // delegate custom models consuming if necessary
                if (modelsConsumer != null) {
                    modelsConsumer.accept(validation, models);
                }
            });

            // save file checksum and validation, if validation is OK
            if (!validationModel.hasErrors()) {
                String newToken = TokenGenerator.getValidationToken(5, ChronoUnit.MINUTES, Collections.emptyMap());
                validationModel.setValidationToken(newToken);
                validationCache.put(checksum, validationModel);
            }

        } // import a previously validated file
        else if (!validOnly) {

            // ensure token validity
            if (!validationModel.getValidationToken().equals(token)) {
                throw new IllegalArgumentException("Bad validation token : " + token);
            }

            // remove file declaration and content from validation cache, since file content will be imported
            validationCache.invalidate(checksum);

            // performs import by bypassing validation, use previously collected objects
            fallback.create(validationModel, (List<T>) validationModel.getObjects());
            validationModel.getObjects().clear();
        }
        return validationModel;
    }

    @Override
    public void create(CSVValidationModel validation, List<T> models) throws Exception {
        fallback.create(validation, models);
    }
}



