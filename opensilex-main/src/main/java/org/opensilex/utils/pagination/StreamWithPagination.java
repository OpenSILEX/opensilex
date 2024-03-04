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

    private final Stream<T> stream;

    /**
     * @param stream the Stream of objects
     * @throws IllegalArgumentException if stream is null (Use {@link Stream#empty()} or {@link StreamWithPagination#StreamWithPagination()} instead
     */
    public StreamWithPagination(Stream<T> stream, int total, int page, int pageSize) throws IllegalArgumentException{
        super(total, page, pageSize);
        Objects.requireNonNull(stream);
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

    @Override
    public Stream<T> getSource() {
        return stream;
    }

}
