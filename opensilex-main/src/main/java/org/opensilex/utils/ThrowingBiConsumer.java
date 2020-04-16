//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to allow bi-consumer usage with exceptions in it.
 *
 * @author Vincent Migot
 * @param <T> BiConsumer first parameter class
 * @param <U> BiConsumer second parameter class
 * @param <E> Swallowed exceptions
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Exception> {

    /**
     * Class Logger.
     */
    final static Logger LOGGER = LoggerFactory.getLogger(ThrowingBiConsumer.class);

    /**
     * BiConsumer execution.
     *
     * @param t consumed key
     * @param u consumed value
     * @throws E exception swallowed
     */
    void accept(T t, U u) throws E;

    /**
     * Helper method to create a consumer swallowing exceptions.
     *
     * @param <T> BiConsumer first parameter class
     * @param <U> BiConsumer second parameter class
     * @param <E> Swallowed exceptions
     * @param throwingConsumer bi-consumer to execute
     * @param keyClass BiConsumer first parameter class
     * @param valueClass BiConsumer second parameter class
     * @param exceptionClass exception class to swallow
     * @return BiConsumer swallowing exceptions
     */
    static <T, U, E extends Exception> BiConsumer<T, U> wrap(
            ThrowingBiConsumer<T, U, E> throwingConsumer,
            Class<T> keyClass,
            Class<U> valueClass,
            Class<E> exceptionClass
    ) {

        return (i, j) -> {
            try {
                throwingConsumer.accept(i, j);
            } catch (Exception ex) {
                LOGGER.warn("Unexpected silent exception:", ex);
                try {
                    exceptionClass.cast(ex);
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }
}
