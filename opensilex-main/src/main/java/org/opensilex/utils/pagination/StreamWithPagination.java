package org.opensilex.utils.pagination;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class StreamWithPagination<T> extends PaginatedIterable<T> {

    private final Stream<T> stream;

    public StreamWithPagination(Stream<T> stream, int total, int page, int pageSize) {
        super(total,page,pageSize);
        this.stream = stream;
    }

    public StreamWithPagination(){
        super(0,0,0);
        stream = Stream.empty();
    }
    @Override
    public void forEach(Consumer<T> action) {
        stream.forEach(action);
    }

}
