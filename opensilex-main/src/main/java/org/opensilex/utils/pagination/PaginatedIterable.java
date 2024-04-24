package org.opensilex.utils.pagination;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opensilex.server.response.PaginationDTO;

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

    protected final PaginationDTO pagination;

    // The pagination strategy used to fetch the source
    protected PaginatedSearchStrategy paginationStrategy;

    /**
     * @param source the iterable source of T
     * @param page current page
     * @param pageSize page size
     * @param total total elements count
     * @throws IllegalArgumentException if page, total or pageSize is < 0
     */
    protected PaginatedIterable(S source, long page, long pageSize, long total, long countLimit) {
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
        this.pagination = new PaginationDTO(pageSize, page, total, countLimit);
    }

    protected PaginatedIterable(S source, long page, long pageSize, boolean hasNextPage) {
        this.source = source;
        this.pagination = new PaginationDTO(pageSize, page, hasNextPage);
    }

    public long getTotal() {
        return pagination.getTotalCount();
    }

    public long getPage() {
        return pagination.getCurrentPage();
    }

    public long getPageSize() {
        return pagination.getPageSize();
    }

    public long getLimitCount() {
        return pagination.getLimitCount();
    }

    public PaginationDTO getPagination() {
        return pagination;
    }

    public abstract void forEach(Consumer<T> action);

    /**
     * @return the effective objects which allow traversal of T items
     */
    public S getSource(){
        return source;
    }

    @JsonIgnore
    public PaginatedSearchStrategy getPaginationStrategy() {
        return paginationStrategy;
    }

    public PaginatedIterable<T, S> setPaginationStrategy(PaginatedSearchStrategy paginationStrategy) {
        this.paginationStrategy = paginationStrategy;
        return this;
    }
}
