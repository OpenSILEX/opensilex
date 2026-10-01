//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.aiimport.report.ReportMessage;
import org.opensilex.aiimport.workbook.HeaderMatcher;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the user has told us a misspelt name means, for the length of one conversation.
 * <p>
 * The file is not rewritten and nothing is renamed in the instance: "Chardonay" still reads
 * "Chardonay" in the spreadsheet, and the germplasm is still "Chardonnay". What changes is that the
 * resolution now knows the two are the same, because a person said so — which is the only
 * authority this module accepts for that kind of statement.
 * <p>
 * Keyed on the normalised value, so the confirmation holds for every row that writes the name the
 * same way, whatever its case or separators.
 *
 * @author Arnaud Charleroy
 */
public class ConfirmedMatches {

    private final Map<ReportCategory, Map<String, ResourceReference>> byCategory =
            new EnumMap<>(ReportCategory.class);

    public void confirm(ReportCategory category, String fileValue, ResourceReference resource) {
        byCategory.computeIfAbsent(category, key -> new HashMap<>())
                .put(HeaderMatcher.normalize(fileValue), resource);
    }

    /**
     * @return true when there was a confirmation to forget
     */
    public boolean forget(ReportCategory category, String fileValue) {
        Map<String, ResourceReference> confirmed = byCategory.get(category);
        return confirmed != null && confirmed.remove(HeaderMatcher.normalize(fileValue)) != null;
    }

    public Optional<ResourceReference> lookup(ReportCategory category, String fileValue) {
        Map<String, ResourceReference> confirmed = byCategory.get(category);
        if (confirmed == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(confirmed.get(HeaderMatcher.normalize(fileValue)));
    }

    /**
     * Resolves an item from its confirmation, when it has one.
     * <p>
     * The item comes out exactly as a correctly spelt name would — found, with its match — plus a
     * flag and a hint saying that a person made the link, so the interface can show it and let
     * them take it back.
     *
     * @return true when the item is settled and needs no search
     */
    public boolean resolve(ReportCategory category, ResolvedItem item) {
        Optional<ResourceReference> match = lookup(category, item.getSourceValue());
        if (!match.isPresent()) {
            return false;
        }
        String name = match.get().getName();
        item.setStatus(ResolutionStatus.FOUND)
                .setConfirmedByUser(true)
                .setMatches(new ArrayList<>(List.of(new ResourceReference(match.get().getUri(), name))))
                .setHint(ReportMessage.of("AiImport.report.hint.confirmedByUser",
                                "Matched to '" + name + "', as the user confirmed; the file writes "
                                        + "it differently.")
                        .with("name", name));
        return true;
    }

    /**
     * Every confirmation, keyed by the normalised name of the file, for storing the session.
     * Confirming the key again restores it: normalising a normalised name changes nothing.
     */
    public List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        byCategory.forEach((category, confirmed) -> confirmed.forEach((key, resource) ->
                entries.add(new Entry(category, key, resource))));
        return entries;
    }

    public static final class Entry {

        private final ReportCategory category;
        private final String fileValue;
        private final ResourceReference resource;

        Entry(ReportCategory category, String fileValue, ResourceReference resource) {
            this.category = category;
            this.fileValue = fileValue;
            this.resource = resource;
        }

        public ReportCategory getCategory() {
            return category;
        }

        public String getFileValue() {
            return fileValue;
        }

        public ResourceReference getResource() {
            return resource;
        }
    }

    public boolean isEmpty() {
        return byCategory.values().stream().allMatch(Map::isEmpty);
    }
}
