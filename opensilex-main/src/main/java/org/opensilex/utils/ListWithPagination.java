//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import org.opensilex.utils.pagination.PaginatedIterable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper class to define a paginated list.
 *
 * @author Vincent Migot
 * @param <T> Generic list parameter
 */
public class ListWithPagination<T> extends PaginatedIterable<T, List<T>> {

    /**
     * List content for current page.
     */
    private final List<T> list;

    public ListWithPagination(List<T> list) {
        this(list, 0, 0, list.size());
    }

    public ListWithPagination(List<T> list, int page, int pageSize, int total) {
        super(total, page, pageSize);
        Objects.requireNonNull(list);
        this.list = list;
    }

    public ListWithPagination(List<T> list, Integer page, Integer pageSize, int total) {
        this(list,
                page == null || page < 0 ? 0 : page,
                pageSize == null || pageSize < 0 ? 0 : pageSize,
                total);
    }

    /**
     * Get list of elements for current page.
     *
     * @return list of elements
     */
    public List<T> getList() {
        return list;
    }

    /**
     * Method to convert a paginated list into another.
     *
     * @param <U> Conversion result class
     * @param resultClass Conversion result class
     * @param converter Method to convert from current model to new one
     * @return new paginated list of conversion result class
     */
    public <U> ListWithPagination<U> convert(Class<U> resultClass, Function<T, U> converter) {

        List<U> resultList = list.stream()
                .map(converter)
                .collect(Collectors.toList());

        return new ListWithPagination<>(resultList, this.page, this.pageSize, this.total);
    }

    @Override
    public void forEach(Consumer<T> action) {
        list.forEach(action);
    }

    @Override
    public List<T> getSource() {
        return getList();
    }
}
