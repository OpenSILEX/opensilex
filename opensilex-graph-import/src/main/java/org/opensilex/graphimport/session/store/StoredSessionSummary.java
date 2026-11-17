//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session.store;

import java.time.Instant;

/**
 * A stored session as a list shows it, without its state.
 *
 * @author Arnaud Charleroy
 */
public record StoredSessionSummary(String id, String title, Instant createdAt, Instant updatedAt) {
}
