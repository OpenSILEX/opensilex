//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session;

import java.time.Instant;

/**
 * A creation step that wrote to the instance: kept with the import, so that the user sees what was
 * already created.
 *
 * @param step    the step, as its writer names it
 * @param at      when it wrote
 * @param created how many resources it created
 * @author Arnaud Charleroy
 */
public record CreationRecord(String step, Instant at, int created) {
}
