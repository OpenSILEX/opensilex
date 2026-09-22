//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log;

import java.util.List;

/**
 * Decides whether a request path is logged.
 *
 * <p>Excluding {@code monitoring} is not an optimisation, it is a correctness requirement: without
 * it, reading the activity chart inflates the activity chart, and an external probe polling the
 * health endpoint ends up dominating the collection.</p>
 *
 * @author Arnaud Charleroy
 */
public class PathExclusionMatcher {

    private final String[] prefixes;

    public PathExclusionMatcher(List<String> configuredPrefixes) {
        this.prefixes = configuredPrefixes == null
                ? new String[0]
                : configuredPrefixes.stream()
                        .filter(p -> p != null && !p.isBlank())
                        .map(PathExclusionMatcher::normalize)
                        .toArray(String[]::new);
    }

    public boolean isExcluded(String path) {
        if (path == null) {
            return false;
        }
        String normalized = normalize(path);
        for (String prefix : prefixes) {
            if (normalized.equals(prefix) || normalized.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String result = value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
