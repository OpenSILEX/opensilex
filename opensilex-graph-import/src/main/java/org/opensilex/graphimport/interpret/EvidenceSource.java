//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import java.net.URI;
import java.util.List;

/**
 * Finds evidence about the property a column holds. Stateless: the same column gives the same clues.
 * A module adds a source through {@link org.opensilex.graphimport.GraphImportExtension}.
 *
 * @author Arnaud Charleroy
 */
public interface EvidenceSource {

    /** What the clues of a source may do. */
    enum Role {
        /** A clue settles the column alone, such as a header written as the predicate. */
        DECIDES,
        /** A clue makes a candidate. */
        PROPOSES,
        /** A clue only weighs on candidates other sources made. */
        MODULATES
    }

    /** A short stable name, shown with each clue and used to order the sources. */
    String id();

    Role role();

    /**
     * @return clues about properties the column's class accepts; none when the source has nothing to
     *         say, which is not the same as evidence against
     */
    List<Clue<URI>> gather(ColumnContext column) throws Exception;
}
