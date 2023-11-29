package org.opensilex.utils.pagination;

import java.util.stream.Stream;

public class StreamWithPagination<T> {

    private final Stream<T> stream;

    /**
     * Total number of elements.
     */
    private final int total;

    /**
     * Current page.
     */
    private final int page;

    /**
     * Page size.
     */
    private final int pageSize;

    public StreamWithPagination(Stream<T> stream, int total, int page, int pageSize) {
        this.stream = stream;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public StreamWithPagination(){
        stream = Stream.empty();
        total = page = pageSize = 0;
    }

    public Stream<T> getStream() {
        return stream;
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
}
