package org.opensilex.integration.test;

import org.opensilex.server.response.JsonResponse;

/**
 * Simple implementation of {@link JsonResponse} for deserialization purposes
 * @param <T> the type of the "result" part of the response
 */
public class DeserializedResponse<T> extends JsonResponse<T> {

}
