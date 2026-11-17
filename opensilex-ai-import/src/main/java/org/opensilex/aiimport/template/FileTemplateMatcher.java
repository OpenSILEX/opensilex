//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Tells how much a sheet looks like one a template describes, from the headers alone: accents and
 * case do not matter, and a column more or less lowers the score without ruling the template out.
 *
 * @author Arnaud Charleroy
 */
public final class FileTemplateMatcher {

    /**
     * Below this share of shared headers a sheet is not proposed a template: a guess this thin
     * would only be noise to dismiss.
     */
    public static final double THRESHOLD = 0.6;

    /**
     * @param recognised how many of the sheet's headers the template's sheet has
     * @param total      how many headers the sheet has
     * @param score      the share of the headers of both that they have in common, from 0 to 1
     */
    public record Match(FileTemplate template, TemplateSheet sheet, double score, int recognised, int total) {
    }

    private FileTemplateMatcher() {
    }

    /**
     * @return the best template sheet for this sheet that reaches the threshold, or empty when none does
     */
    public static Optional<Match> best(SheetStructure sheet, List<FileTemplate> templates) {
        Set<String> own = normalised(sheet.getHeaders());
        Match best = null;
        for (FileTemplate template : templates) {
            for (TemplateSheet candidate : template.getSheets()) {
                Set<String> theirs = normalised(candidate.getHeaders());
                Set<String> shared = new HashSet<>(own);
                shared.retainAll(theirs);
                Set<String> all = new HashSet<>(own);
                all.addAll(theirs);
                double score = all.isEmpty() ? 0 : (double) shared.size() / all.size();
                if (score >= THRESHOLD && (best == null || score > best.score())) {
                    best = new Match(template, candidate, score, shared.size(), own.size());
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static Set<String> normalised(List<String> headers) {
        Set<String> set = new HashSet<>();
        headers.stream().filter(header -> header != null && !header.isBlank())
                .forEach(header -> set.add(HeaderMatcher.normalize(header)));
        return set;
    }
}
