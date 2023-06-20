package org.opensilex.utils.functionnal;

@FunctionalInterface
public interface ThrowingSupplier<T,E extends Exception> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E;
}
