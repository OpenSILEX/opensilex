//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret.alias;

import java.net.URI;
import java.util.Optional;

/**
 * The properties a user chose before for a header, for a class.
 *
 * @author Arnaud Charleroy
 */
@FunctionalInterface
public interface LearnedAliases {

    /** Knows no alias. */
    LearnedAliases NONE = (owner, classUri, header) -> Optional.empty();

    /**
     * @param header the header in {@code HeaderMatcher.normalize} form
     * @return the property this user chose most often for that header and class
     */
    Optional<URI> find(URI owner, URI classUri, String header);
}
