package org.opensilex.utils.pagination;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Class which define any Object which can be Iterable and provides pagination mechanism after a Database Search request
 * @param <T> The type of elements which are iterable
 * @param <S> Type of class which is iterable on T element
 */
public abstract class PaginatedIterable<T, S>{

    /**
     * The iterable of T
     */
    protected final S source;

    /**
     * Current page.
     */
    protected final int page;

    /**
     * Page size.
     */
    protected final int pageSize;

    /**
     * Total number of element
     */
    protected final int total;

    /**
     * Indicate if the count query was used with a limit on the number of element to count.
     * This can be done for performance reason, in order to not iterate each document to count, when this number becomes high
     */
    protected final int countLimit;

    /**
     * @param source the iterable source of T
     * @param page current page
     * @param pageSize page size
     * @param total total elements count
     * @throws IllegalArgumentException if page, total or pageSize is < 0
     */
    protected PaginatedIterable(S source, int page, int pageSize, int total, int countLimit) {
        Objects.requireNonNull(source);
        if(page < 0){
            throw new IllegalArgumentException("page must be >= 0");
        }
        if(pageSize < 0){
            throw new IllegalArgumentException("pageSize must be >= 0");
        }
        if(total < 0){
            throw new IllegalArgumentException("total must be >= 0");
        }
        if(countLimit < 0){
            throw new IllegalArgumentException("countLimit must be >= 0");
        }
        this.source = source;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.countLimit = countLimit;
    }

    /**
     * @see #PaginatedIterable(Object, int, int, int, int)
     */
    protected PaginatedIterable(S source, int page, int pageSize, int total) {
        this(source, page, pageSize, total, 0);
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
