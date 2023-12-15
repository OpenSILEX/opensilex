package org.opensilex.utils.pagination;

import java.util.function.Consumer;

public abstract class PaginatedIterable<T>{

    protected final int total;

    /**
     * Current page.
     */
    protected final int page;

    /**
     * Page size.
     */
    protected final int pageSize;

    /**
     * @param page current page
     * @param pageSize page size
     * @param total total elements count
     */
    protected PaginatedIterable(int total, int page, int pageSize) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
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

    public abstract void forEach(Consumer<T> action);

}
