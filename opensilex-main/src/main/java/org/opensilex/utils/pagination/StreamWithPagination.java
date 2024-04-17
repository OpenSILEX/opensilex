package org.opensilex.utils.pagination;

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
     * Constructor for an empty stream. Just keep the information about the provided pagination
     */
    public StreamWithPagination(long page, long pageSize){
        super(Stream.empty(), page, pageSize, 0, 0);
    }

    /**
     /**
     * Constructor for a non-empty list, with information about provided pagination and the counted element number
     *
     * @param stream the Stream of objects
     * @throws IllegalArgumentException if stream is null (Use {@link Stream#empty()} instead
     */
    public StreamWithPagination(Stream<T> stream, long page, long pageSize, long total) throws IllegalArgumentException {
       this(stream, page, pageSize, total, 0);
    }

    /**
     * Constructor for a non-empty stream, with information about provided pagination, the counted element number and the count limit
     * which has applied
     */
    public StreamWithPagination(Stream<T> stream, long page, long pageSize, long total, long countLimit) {
        super(stream, page, pageSize, total, countLimit);
    }

    /**
     /**
     * Constructor for a non-empty list, with information about provided pagination and the counted element number
     *
     * @param stream the Stream of objects
     * @throws IllegalArgumentException if stream is null (Use {@link Stream#empty()} instead
     */
    public StreamWithPagination(Stream<T> stream, long page, long pageSize) throws IllegalArgumentException {
        super(stream, page, pageSize, false);
    }

    @Override
    public void forEach(Consumer<T> action) {
        getSource().forEach(action);
    }


}
