//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.dal;

import java.net.URI;

/**
 * A kingdom: individual of {@link org.opensilex.core.ontology.Oeso#Kingdom}, delivered with the ontology.
 *
 * @param uri   kingdom URI (e.g. oeso:Plantae)
 * @param label label in the requested language
 */
public record Kingdom(URI uri, String label) {
}
