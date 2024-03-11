package org.opensilex.utils.pagination;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * <pre>
 * Iterable object based on a {@link Stream}.
 * This object provide a way to iterate objects from a database paginated request without having to materialize these objects in some collection.
 * This allows to save space when only lookup or mapping is required (ex: when converting Models from database to a list of DTO)
 * </pre>
 *
 * @param <T> Generic Stream parameter
 * @author rcolin
 */
public class StreamWithPagination<T> extends PaginatedIterable<T, Stream<T>> {

    /**
     * @param stream the Stream of objects
     * @throws IllegalArgumentException if stream is null (Use {@link Stream#empty()} instead
     */
    public StreamWithPagination(Stream<T> stream, long page, long pageSize,long total) throws IllegalArgumentException{
        super(stream, total, page, pageSize, total);
    }

    public StreamWithPagination(Stream<T> stream, long page, long pageSize) throws IllegalArgumentException{
        super(stream, page, pageSize, 0);
    }

    @Override
    public void forEach(Consumer<T> action) {
        getSource().forEach(action);
    }


}
