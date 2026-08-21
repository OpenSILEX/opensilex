package org.opensilex.server.rest.swagger;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RemoveRestPrefixFilter extends AbstractSpecFilter {

    private static final String PREFIX = "/rest";

    @Override
    public Optional<OpenAPI> filterOpenAPI(@NotNull OpenAPI openAPI, Map<String, List<String>> params,
                                           Map<String, String> cookies, Map<String, List<String>> headers) {
        if (openAPI.getPaths() == null) {
            return null;
        }

        Paths newPaths = new Paths();
        openAPI.getPaths().forEach((path, item) -> {
            String cleaned = path.startsWith(PREFIX) ? path.substring(PREFIX.length()) : path;
            if (cleaned.isEmpty()) {
                cleaned = "/";
            }
            newPaths.addPathItem(cleaned, item);
        });
        openAPI.setPaths(newPaths);
        return Optional.of(openAPI);
    }
}