//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Exception> {

    final static Logger LOGGER = LoggerFactory.getLogger(ThrowingBiConsumer.class);

    void accept(T t, U u) throws E;

    static <T, U, E extends Exception> BiConsumer<T, U> wrap(ThrowingBiConsumer<T, U, E> throwingConsumer, Class<E> exceptionClass) {

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
