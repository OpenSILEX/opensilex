//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.ontology.HeaderReading;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Finds the column whose values identify the rows of a sheet, before its class is known: rows of two
 * sheets sharing a value of their identifier column are one node.
 * <p>
 * A repeated value is allowed, since a sheet may give several rows to one node; a column of unique
 * values is only more likely to be the identifier.
 *
 * @author Arnaud Charleroy
 */
public final class IdentifierDetector {

    public static final String SOURCE = "identifier";
    static final Combination.Bar BAR = new Combination.Bar(0.6, 0.2);
    static final double HEADER = 0.4;
    static final double URIS = 0.2;
    static final double UNIQUE = 0.3;
    static final double UNIQUE_INCOMPLETE = 0.15;
    static final double FIRST = 0.1;

    private static final Pattern MEASURE = Pattern.compile("^[+-]?\\d+[.,]\\d+$|^\\d{4}-\\d{2}-\\d{2}.*$");
    private static final Pattern ABSOLUTE_URI = Pattern.compile("^https?://\\S+$");
    private static final URI NAME = URI.create(RDFS.label.getURI());

    private IdentifierDetector() {
    }

    /**
     * @param readings what each header says by its form, one per column
     */
    public static Choice<Integer> detect(SheetStructure sheet, List<HeaderReading> readings) {
        List<Clue<Integer>> proposals = new ArrayList<>();
        List<Clue<Integer>> modulations = new ArrayList<>();
        for (int column = 0; column < sheet.getHeaders().size(); column++) {
            HeaderReading reading = readings.get(column);
            if (reading.form() == HeaderReading.Form.TYPE) {
                continue;
            }
            List<String> values = ColumnValues.all(sheet, column);
            List<String> present = values.stream().filter(Objects::nonNull).toList();
            if (present.isEmpty() || present.stream().allMatch(value -> MEASURE.matcher(value).matches())) {
                continue;
            }
            String header = sheet.getHeaders().get(column);
            if (CommonHeaderWords.namesIdentifier(HeaderMatcher.normalize(header)) || NAME.equals(reading.property())) {
                proposals.add(Clue.of(column, SOURCE, HEADER, Reasons.identifierHeader(header)));
            }
            if (present.stream().allMatch(value -> ABSOLUTE_URI.matcher(value).matches())) {
                proposals.add(Clue.of(column, SOURCE, URIS, Reasons.identifierUris()));
            }
            Set<String> distinct = new HashSet<>(present);
            if (distinct.size() == present.size()) {
                proposals.add(present.size() == values.size()
                        ? Clue.of(column, SOURCE, UNIQUE, Reasons.uniqueValues())
                        : Clue.of(column, SOURCE, UNIQUE_INCOMPLETE, Reasons.uniqueButIncomplete()));
            }
            if (column == 0) {
                modulations.add(Clue.of(column, SOURCE, FIRST, Reasons.firstColumn()));
            }
        }
        return Choice.fromEvidence(Combination.rank(proposals, modulations, Comparator.naturalOrder()), BAR, List.of());
    }
}
