//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.aiimport.resolve.NameSimilarity;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.HeaderReading;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeSet;

/**
 * Suggests the class of the rows of a sheet from its name, its headers, a type column, and the
 * identifiers that already name existing instances.
 *
 * @author Arnaud Charleroy
 */
public class SheetClassSuggester {

    public static final String SOURCE = "class";
    static final Combination.Bar BAR = new Combination.Bar(0.5, 0.15);
    static final double SAME_NAME = 0.6;
    static final double CLOSE_NAME = 0.5;
    static final double COVERAGE = 0.5;
    static final double EXISTING = 0.5;
    static final double MIN_SHARE = 0.5;
    /** How many classes found by the sheet's name, besides the roots, get their headers compared. */
    static final int NAME_MATCHED_VIEWS = 3;

    private static final Comparator<URI> BY_URI = Comparator.comparing(URI::toString);

    private final InterpretationServices services;

    public SheetClassSuggester(InterpretationServices services) {
        this.services = services;
    }

    /** The class of a sheet, and the classes its type column gives each row, if it has one. */
    public record SheetClass(Choice<URI> choice, Set<URI> rowClasses) {
    }

    /**
     * @param readings   what each header says by its form, one per column
     * @param typeColumn the column giving each row's class, or null
     * @param identifier the identifier column of the sheet
     */
    public SheetClass suggest(SheetStructure sheet, List<HeaderReading> readings, Integer typeColumn,
                              Choice<Integer> identifier) throws Exception {
        if (typeColumn != null) {
            Optional<SheetClass> typed = fromTypeColumn(sheet, typeColumn);
            if (typed.isPresent()) {
                return typed.get();
            }
        }
        List<Clue<URI>> clues = new ArrayList<>(byName(sheet.getName()));
        Set<URI> compared = new LinkedHashSet<>(sortedRoots());
        clues.stream().sorted(Comparator.comparingDouble((Clue<URI> clue) -> -clue.evidence().weight())
                        .thenComparing(Clue::target, BY_URI))
                .limit(NAME_MATCHED_VIEWS)
                .forEach(clue -> compared.add(clue.target()));
        for (URI classUri : compared) {
            coverage(classUri, sheet, readings).ifPresent(clues::add);
        }
        if (identifier.chosen() != null) {
            clues.addAll(existingRows(sheet, identifier.chosen()));
        }
        return new SheetClass(Choice.fromEvidence(Combination.rank(clues, List.of(), BY_URI), BAR, List.of()), Set.of());
    }

    private Optional<SheetClass> fromTypeColumn(SheetStructure sheet, int typeColumn) {
        Set<URI> rowClasses = new TreeSet<>(BY_URI);
        Set<String> unresolved = new TreeSet<>();
        for (String value : ColumnValues.distinct(sheet, typeColumn, ColumnValues.EVIDENCE_SAMPLE)) {
            URI classUri = classNamed(value);
            if (classUri == null) {
                unresolved.add(value);
            } else {
                rowClasses.add(classUri);
            }
        }
        URI common = rowClasses.isEmpty() || !unresolved.isEmpty() ? null : services.catalog().commonClass(rowClasses);
        if (common == null) {
            if (unresolved.isEmpty()) {
                return Optional.empty();
            }
            Choice<URI> undecided = Choice.undecided(List.of(),
                    List.of(Reasons.unresolvedTypes(String.join(", ", unresolved))));
            return Optional.of(new SheetClass(undecided, rowClasses));
        }
        Evidence evidence = new Evidence(SOURCE, 1, Reasons.typeColumn(label(common)));
        Choice<URI> decided = new Choice<>(common, Choice.Status.DECIDED,
                List.of(new Candidate<>(common, 1, List.of(evidence))), List.of());
        return Optional.of(new SheetClass(decided, rowClasses));
    }

    /**
     * The catalog class a type cell names, by URI, prefixed name or label.
     *
     * @return null when it names none
     */
    public URI classNamed(String value) {
        HeaderReading reading = services.headers().read(value);
        if (reading.names()) {
            URI classUri = Uris.expand(reading.property());
            return services.catalog().contains(classUri) ? classUri : null;
        }
        String normalized = HeaderMatcher.normalize(value);
        for (URI classUri : services.catalog().getClasses()) {
            if (namesOf(classUri).stream().anyMatch(name -> normalized.equals(HeaderMatcher.normalize(name)))) {
                return classUri;
            }
        }
        return null;
    }

    private List<Clue<URI>> byName(String sheetName) {
        String normalized = singular(HeaderMatcher.normalize(sheetName));
        List<Clue<URI>> clues = new ArrayList<>();
        if (normalized.isEmpty()) {
            return clues;
        }
        for (URI classUri : services.catalog().getClasses()) {
            double best = 0;
            for (String name : namesOf(classUri)) {
                if (normalized.equals(singular(HeaderMatcher.normalize(name)))) {
                    best = SAME_NAME;
                    break;
                }
                OptionalDouble similarity = NameSimilarity.similarity(normalized, singular(HeaderMatcher.normalize(name)));
                if (similarity.isPresent()) {
                    best = Math.max(best, CLOSE_NAME * similarity.getAsDouble());
                }
            }
            if (best > 0) {
                clues.add(Clue.of(classUri, SOURCE, best, Reasons.sheetName(sheetName, label(classUri))));
            }
        }
        return clues;
    }

    private Optional<Clue<URI>> coverage(URI classUri, SheetStructure sheet, List<HeaderReading> readings)
            throws Exception {
        ClassView view = services.views().get(classUri);
        int recognised = 0;
        int total = 0;
        for (int column = 0; column < readings.size(); column++) {
            HeaderReading reading = readings.get(column);
            if (reading.form() == HeaderReading.Form.TYPE) {
                continue;
            }
            total++;
            if (reading.names() ? view.getProperty(reading.property()) != null
                    : namesAProperty(view, sheet.getHeaders().get(column))) {
                recognised++;
            }
        }
        if (recognised == 0) {
            return Optional.empty();
        }
        return Optional.of(Clue.of(classUri, SOURCE, COVERAGE * recognised / total,
                Reasons.headerCoverage(recognised, total, label(classUri))));
    }

    private static boolean namesAProperty(ClassView view, String header) {
        String normalized = HeaderMatcher.normalize(header);
        if (normalized.isEmpty()) {
            return false;
        }
        Optional<URI> common = CommonHeaderWords.propertyFor(normalized);
        if (common.isPresent() && view.getProperty(common.get()) != null) {
            return true;
        }
        for (PropertyView property : view.getProperties()) {
            List<String> names = new ArrayList<>(property.labels().all());
            names.add(ClassCatalog.localName(property.uri()));
            if (names.stream().anyMatch(name -> normalized.equals(HeaderMatcher.normalize(name)))) {
                return true;
            }
        }
        return false;
    }

    private List<Clue<URI>> existingRows(SheetStructure sheet, int identifierColumn) throws Exception {
        List<String> identifiers = ColumnValues.distinct(sheet, identifierColumn, ColumnValues.EVIDENCE_SAMPLE);
        List<Clue<URI>> clues = new ArrayList<>();
        if (identifiers.isEmpty()) {
            return clues;
        }
        for (URI root : sortedRoots()) {
            Map<String, Set<URI>> found = services.instances().lookUp(root, identifiers);
            long named = identifiers.stream().filter(value -> found.containsKey(InstanceIndex.key(value))).count();
            double share = (double) named / identifiers.size();
            if (share >= MIN_SHARE) {
                clues.add(Clue.of(root, SOURCE, EXISTING * share,
                        Reasons.existingRows((int) named, identifiers.size(), label(root))));
            }
        }
        return clues;
    }

    private Set<URI> sortedRoots() {
        Set<URI> roots = new TreeSet<>(BY_URI);
        roots.addAll(services.catalog().getRoots());
        return roots;
    }

    private List<String> namesOf(URI classUri) {
        List<String> names = new ArrayList<>(services.catalog().labelsOf(classUri).all());
        names.add(ClassCatalog.localName(classUri));
        return names;
    }

    private String label(URI classUri) {
        return services.catalog().labelOf(classUri, services.language());
    }

    /** "parcelles" and "parcelle", "plots" and "plot", "reseaux" and "reseau" compare equal. */
    private static String singular(String normalized) {
        if (normalized.length() > 3 && (normalized.endsWith("s") || normalized.endsWith("x"))) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
