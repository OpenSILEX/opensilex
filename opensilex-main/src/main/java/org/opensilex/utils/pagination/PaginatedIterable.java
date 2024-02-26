package org.opensilex.utils.pagination;

import java.util.function.Consumer;

/**
 * Class which define any Object which can be Iterable and provides pagination mechanism after a Database Search request
 * @param <T> The type of elements which are iterable
 */
public abstract class PaginatedIterable<T, S>{

    /**
     * Total number of element
     */
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
     * Indicate if the count query was used with a limit on the number of element to count.
     * This can be done for performance reason, in order to not iterate each document to count, when this number becomes high
     */
    protected final int countLimit;

    protected final S source;

    protected PaginatedIterable(S source, int total, int page, int pageSize, int countLimit) {
        if(total < 0){
            throw new IllegalArgumentException("total must be >= 0");
        }
        if(page < 0){
            throw new IllegalArgumentException("page must be >= 0");
        }
        if(pageSize < 0){
            throw new IllegalArgumentException("pageSize must be >= 0");
        }
        if(countLimit < 0){
            throw new IllegalArgumentException("countLimit must be >= 0");
        }
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.countLimit = countLimit;
        this.source = source;
    }

    /**
     * @param page current page
     * @param pageSize page size
     * @param total total elements count
     * @throws IllegalArgumentException if page, total or pageSize is < 0
     */
    protected PaginatedIterable(S source, int total, int page, int pageSize) throws IllegalArgumentException {
        this(source, total, page, pageSize, 0);
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

    public int getCountLimit() {
        return countLimit;
    }

    public abstract void forEach(Consumer<T> action);

    /**
     * @return the effective objects which allow traversal of T items
     */
    public S getSource(){
        return source;
    }
}
