//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import org.opensilex.utils.pagination.PaginatedIterable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class to define a paginated list.
 *
 * @author Vincent Migot
 * @param <T> Generic list parameter
 */
public class ListWithPagination<T> extends PaginatedIterable<T, List<T>> {

    /**
     * Constructor for an empty list. Just keep the information about the provided pagination
     */
    public ListWithPagination(long page, long pageSize){
        super(Collections.emptyList(),page, pageSize,0,0);
    }

    public ListWithPagination(List<T> list) {
        this(list, 0, 0, list.size());
    }

    /**
     * Constructor for a non-empty list, with information about provided pagination and the counted element number
     */
    public ListWithPagination(List<T> list, long page, long pageSize, long total) {
        this(list, page, pageSize, total, 0);
    }

    /**
     * Constructor for a non-empty list, with information about provided pagination, the counted element number and the count limit
     * which has applied
     */
    public ListWithPagination(List<T> list,long page, long pageSize, long total, long countLimit) {
        super(list, page, pageSize, total, countLimit);
    }

    /**
     * Constructor for a non-empty list, with information about provided pagination, and a flag about
     */
    public ListWithPagination(List<T> list,long page, long pageSize, boolean hasNextElement) {
        super(list,page, pageSize, hasNextElement);
    }


    /**
     * Get list of elements for current page.
     *
     * @return list of elements
     */
    public List<T> getList() {
        return getSource();
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

        List<U> resultList = getSource().stream()
                .map(converter)
                .collect(Collectors.toList());

        return new ListWithPagination<>(resultList, this.getPage(), this.getPageSize(), this.getTotal());
    }

    @Override
    public void forEach(Consumer<T> action) {
        getSource().forEach(action);
    }

}
