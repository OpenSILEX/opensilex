//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session.store;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * What {@link SnapshotSessionStore} keeps of a session: who owns it, what to call it in a list, the
 * stored files to delete with it, and its own state, which the store never reads.
 *
 * @param <S>      the session's own state, written as JSON
 * @param id       an identifier the server generated; it makes up the paths of the stored files
 * @param owner    the account the session belongs to
 * @param title    what the session is called in the list of stored sessions
 * @param fileIds  the identifiers of the stored files, so that deleting the session deletes them
 * @param snapshot the session's own state
 * @author Arnaud Charleroy
 */
public record StoredSession<S>(String id, URI owner, String title, Instant createdAt, List<String> fileIds,
                               S snapshot) {
}
