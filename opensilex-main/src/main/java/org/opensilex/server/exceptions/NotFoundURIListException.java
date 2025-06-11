package org.opensilex.server.exceptions;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author rigolle
 */
public class NotFoundURIListException extends NotFoundException{
    public NotFoundURIListException(Stream<URI> uriList) {
        this("At least one URI was not found among " + Arrays.toString(uriList.toArray()), "URI not found among list");
    }

    public NotFoundURIListException(String message, String title) {
        super(message, title);
    }
}
