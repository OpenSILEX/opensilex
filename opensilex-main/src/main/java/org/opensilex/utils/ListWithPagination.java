//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Helper class to define a paginated list.
 *
 * @author Vincent Migot
 * @param <T> Generic list parameter
 */
public class ListWithPagination<T> {

    /**
     * List content for current page.
     */
    private final List<T> list;

    /**
     * Total number of elements.
     */
    private final Integer total;

    /**
     * Current page.
     */
    private final Integer page;

    /**
     * Page size.
     */
    private final Integer pageSize;

    /**
     * Constructor for a complete list without pagination.
     *
     * @param list
     */
    public ListWithPagination(List<T> list) {
        this(list, 0, 0, list.size());
    }

    /**
     * Constructor for a page list.
     *
     * @param list list of element for the current page.
     * @param page current page
     * @param pageSize page size
     * @param total total elements count
     */
    public ListWithPagination(List<T> list, Integer page, Integer pageSize, Integer total) {
        this.list = list;
        this.total = total;

        if (page == null || page < 0) {
            this.page = 0;
        } else {
            this.page = page;
        }

        if (pageSize != null && pageSize > 0) {
            this.pageSize = pageSize;
        } else {
            this.pageSize = pageSize;
        }
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
     * Get total number of elements in the list.
     *
     * @return total count
     */
    public int getTotal() {
        return total;
    }

    /**
     * Get current page index.
     *
     * @return current page
     */
    public int getPage() {
        return page;
    }

    /**
     * Get page size.
     *
     * @return page size
     */
    public int getPageSize() {
        return pageSize;
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
        List<U> resultList = new ArrayList<>(this.list.size());

        this.list.forEach((T element) -> {
            resultList.add(converter.apply(element));
        });

        return new ListWithPagination<U>(resultList, this.page, this.pageSize, this.total);
    }

}
