//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Picks, among the resources an instance already has, the few a misspelt name probably meant.
 * <p>
 * It knows nothing about where the candidates come from, which keeps it testable without an
 * instance: the resolution hands it whatever the DAOs returned, already filtered by what the
 * current user is allowed to see.
 *
 * @author Arnaud Charleroy
 */
public final class NearMatchFinder {

    /**
     * More than three and the list stops being a suggestion and becomes a search result, which the
     * selectors of the platform already do better.
     */
    public static final int MAX_SUGGESTIONS = 3;

    private static final Comparator<ResourceReference> CLOSEST_FIRST =
            Comparator.comparingDouble(ResourceReference::getSimilarity).reversed()
                    .thenComparing(ResourceReference::getName, String.CASE_INSENSITIVE_ORDER);

    /**
     * @param name       the name as the file writes it
     * @param candidates resources of the same kind; one resource may appear under several names —
     *                   a variable's name and its alternative name — and is kept once, at its
     *                   closest
     * @return at most {@link #MAX_SUGGESTIONS} resources, closest first, each carrying its
     *         similarity; empty when nothing is close enough
     */
    public List<ResourceReference> suggest(String name, Collection<ResourceReference> candidates) {
        Map<URI, ResourceReference> closest = new LinkedHashMap<>();
        for (ResourceReference candidate : candidates) {
            if (candidate == null || candidate.getUri() == null || candidate.getName() == null) {
                continue;
            }
            OptionalDouble similarity = NameSimilarity.similarity(name, candidate.getName());
            if (!similarity.isPresent()) {
                continue;
            }
            ResourceReference known = closest.get(candidate.getUri());
            if (known == null || known.getSimilarity() < similarity.getAsDouble()) {
                // A copy: the candidates may be shared between several names of the file.
                closest.put(candidate.getUri(),
                        new ResourceReference(candidate.getUri(), candidate.getName())
                                .setSimilarity(similarity.getAsDouble()));
            }
        }
        List<ResourceReference> suggestions = new ArrayList<>(closest.values());
        suggestions.sort(CLOSEST_FIRST);
        return suggestions.size() > MAX_SUGGESTIONS
                ? new ArrayList<>(suggestions.subList(0, MAX_SUGGESTIONS))
                : suggestions;
    }
}
