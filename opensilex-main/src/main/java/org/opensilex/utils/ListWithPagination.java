//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ListWithPagination<T> {

    private final List<T> list;

    private final Integer total;

    private final Integer page;

    private final Integer pageSize;

    public ListWithPagination(List<T> list) {
        this(list, 0, 0, list.size());
    }
    
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

    public List<T> getList() {
        return list;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public <U> ListWithPagination<U> convert(Class<U> resultClass, Function<T, U> converter) {
        List<U> resultList = new ArrayList<>();

        this.list.forEach((T element) -> {
            resultList.add(converter.apply(element));
        });

        return new ListWithPagination<U>(resultList, this.page, this.pageSize, this.total);
    }

}
