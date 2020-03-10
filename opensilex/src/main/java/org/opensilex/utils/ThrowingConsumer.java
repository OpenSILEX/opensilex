//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

    final static Logger LOGGER = LoggerFactory.getLogger(ThrowingConsumer.class);

    void accept(T t) throws E;

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
