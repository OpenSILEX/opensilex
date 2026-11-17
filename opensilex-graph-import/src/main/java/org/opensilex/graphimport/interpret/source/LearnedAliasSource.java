//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.source;

import org.opensilex.aiimport.workbook.HeaderMatcher;
import org.opensilex.graphimport.interpret.Clue;
import org.opensilex.graphimport.interpret.ColumnContext;
import org.opensilex.graphimport.interpret.EvidenceSource;
import org.opensilex.graphimport.interpret.Reasons;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * What the user chose before for the same header and class.
 *
 * @author Arnaud Charleroy
 */
public class LearnedAliasSource implements EvidenceSource {

    public static final String ID = "alias";
    static final double WEIGHT = 0.8;

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
        Optional<URI> learned = column.services().aliases()
                .find(column.services().owner(), column.classView().getUri(), header);
        if (learned.isEmpty() || column.classView().getProperty(learned.get()) == null) {
            return List.of();
        }
        return List.of(Clue.of(learned.get(), ID, WEIGHT, Reasons.alias(column.header())));
    }
}
