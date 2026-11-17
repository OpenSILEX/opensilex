//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.aiimport.resolve.NameSimilarity;
import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.CommonHeaderWords;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Reasons;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The header compared with the labels of the properties, in every language, and with their local
 * names: the same once case, accents and separators are set aside, or one typo away.
 *
 * @author Arnaud Charleroy
 */
public class LabelSource implements EvidenceSource {

    public static final String ID = "label";
    static final double SAME = 0.7;
    static final double CLOSE = 0.5;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Role role() {
        return Role.PROPOSES;
    }

    @Override
    public List<Clue<URI>> gather(ColumnContext column) {
        String header = HeaderMatcher.normalize(column.header());
        if (header.isEmpty()) {
            return List.of();
        }
        Optional<URI> common = CommonHeaderWords.propertyFor(header);
        List<Clue<URI>> clues = new ArrayList<>();
        for (PropertyView property : column.classView().getProperties()) {
            if (common.isPresent() && common.get().equals(property.uri())) {
                clues.add(Clue.of(property.uri(), ID, SAME,
                        Reasons.commonWord(column.header(), Labelling.of(property, column))));
                continue;
            }
            Clue<URI> best = null;
            for (String name : names(property)) {
                if (header.equals(HeaderMatcher.normalize(name))) {
                    best = Clue.of(property.uri(), ID, SAME, Reasons.label(name));
                    break;
                }
                OptionalDouble similarity = NameSimilarity.similarity(column.header(), name);
                double weight = similarity.isPresent() ? CLOSE * similarity.getAsDouble() : 0;
                if (weight > 0 && (best == null || weight > best.evidence().weight())) {
                    best = Clue.of(property.uri(), ID, weight, Reasons.closeLabel(name));
                }
            }
            if (best != null) {
                clues.add(best);
            }
        }
        return clues;
    }

    private static List<String> names(PropertyView property) {
        List<String> names = new ArrayList<>(property.labels().all());
        names.add(ClassCatalog.localName(property.uri()));
        return names;
    }
}
