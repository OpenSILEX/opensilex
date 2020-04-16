//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to allow consumer usage with exceptions in it.
 *
 * @author Vincent Migot
 * @param <T> Consumer parameter class
 * @param <E> Swallowed exceptions
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

    /**
     * Class Logger.
     */
    final static Logger LOGGER = LoggerFactory.getLogger(ThrowingConsumer.class);

    /**
     * Consumer execution.
     *
     * @param t consumed instance
     * @throws E exception swallowed
     */
    void accept(T t) throws E;

    /**
     * Helper method to create a consumer swallowing exceptions.
     *
     * @param <T> Consumer parameter class
     * @param <E> Swallowed exceptions
     * @param throwingConsumer consumer to execute
     * @param exceptionClass exception class to swallow
     * @return Consumer swallowing exceptions
     */
    static <T, E extends Exception> Consumer<T> wrap(ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionClass) {

        return i -> {
            try {
                throwingConsumer.accept(i);
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
