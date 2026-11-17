//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.graphimport.graph.InstanceIndex;
import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Reasons;
import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Values that name existing instances of the class a link property expects: "96 % of the values are
 * known germplasm". Only instances the user may see count; when fewer than half the values name one,
 * the source says nothing rather than speaking against the property.
 *
 * @author Arnaud Charleroy
 */
public class InstanceValueSource implements EvidenceSource {

    public static final String ID = "instances";
    static final double WEIGHT = 0.8;
    static final double MIN_SHARE = 0.5;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Role role() {
        return Role.PROPOSES;
    }

    @Override
    public List<Clue<URI>> gather(ColumnContext column) throws Exception {
        if (column.identifier() || column.values().isEmpty()) {
            return List.of();
        }
        Map<URI, List<PropertyView>> byRange = new LinkedHashMap<>();
        for (PropertyView property : column.classView().getProperties()) {
            if (property.kind() == PropertyKind.OBJECT && property.range() != null) {
                byRange.computeIfAbsent(property.range(), key -> new ArrayList<>()).add(property);
            }
        }
        List<Clue<URI>> clues = new ArrayList<>();
        for (Map.Entry<URI, List<PropertyView>> range : byRange.entrySet()) {
            Map<String, Set<URI>> found = column.services().instances().lookUp(range.getKey(), column.values());
            int named = 0;
            for (String value : column.values()) {
                if (found.containsKey(InstanceIndex.key(value))) {
                    named++;
                }
            }
            int total = column.values().size();
            double share = (double) named / total;
            if (share >= MIN_SHARE) {
                for (PropertyView property : range.getValue()) {
                    clues.add(Clue.of(property.uri(), ID, WEIGHT * share,
                            Reasons.instances(named, total, Labelling.ofClass(range.getKey(), column))));
                }
            }
        }
        return clues;
    }
}
