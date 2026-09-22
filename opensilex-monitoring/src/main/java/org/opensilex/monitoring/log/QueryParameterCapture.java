//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedMap;
import org.bson.Document;
import org.opensilex.monitoring.config.RequestLogConfig;

/**
 * Turns query parameters into a storable document: masked, capped, and safe for MongoDB keys.
 *
 * @author Arnaud Charleroy
 */
public class QueryParameterCapture {

    private static final String MASK = "***";

    private final Set<String> maskedNames;
    private final int maxParameters;
    private final int maxValueLength;

    public QueryParameterCapture(RequestLogConfig config) {
        this.maskedNames = config.maskedParameterNames() == null
                ? Set.of()
                : config.maskedParameterNames().stream()
                        .filter(name -> name != null && !name.isBlank())
                        .map(name -> name.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet());
        this.maxParameters = Math.max(1, config.maxCapturedParameters());
        this.maxValueLength = Math.max(8, config.maxParameterValueLength());
    }

    /**
     * @return the captured parameters, or {@code null} when there is nothing to store
     */
    public Document capture(MultivaluedMap<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }

        Document document = new Document();
        int kept = 0;
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (kept >= maxParameters) {
                document.put("_truncated", true);
                break;
            }
            String key = sanitizeKey(entry.getKey());
            if (key.isEmpty()) {
                continue;
            }
            document.put(key, maskedNames.contains(entry.getKey().toLowerCase(Locale.ROOT))
                    ? MASK
                    : truncate(entry.getValue()));
            kept++;
        }
        return document.isEmpty() ? null : document;
    }

    /**
     * MongoDB rejects a key starting with a dollar sign and, on older servers, any key containing a
     * dot. A parameter name is user-controlled, so it has to be sanitised before it becomes a key.
     */
    private static String sanitizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().replace('.', '_').replace('$', '_');
    }

    private Object truncate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() == 1) {
            return truncate(values.get(0));
        }
        return values.stream().limit(maxParameters).map(this::truncate).collect(Collectors.toList());
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxValueLength ? value : value.substring(0, maxValueLength) + "…";
    }
}
