//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jvnet.hk2.annotations.Service;
import org.opensilex.graphimport.GraphImportConfig;
import org.opensilex.service.reflection.SelfBound;

import javax.inject.Inject;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Holds the open imports in memory, for the lifetime configured on the module. What leaves the
 * memory is still in the store, and is read back from there when the user returns.
 * <p>
 * The cache is static because self-bound services are request scoped: a per-instance cache would be
 * thrown away between two calls about the same import.
 *
 * @author Arnaud Charleroy
 */
@SelfBound
@Service
public class GraphImportSessionCache {

    /**
     * Bounds how much a burst of uploads can claim. Each entry holds the cells of its files.
     */
    private static final int MAX_SESSIONS = 200;

    private static final int DEFAULT_TTL_MINUTES = 60;

    private static volatile Cache<String, GraphImportSession> store;

    @Inject
    public GraphImportSessionCache(GraphImportConfig config) {
        initialise(config == null ? DEFAULT_TTL_MINUTES : config.sessionTtlMinutes());
    }

    /**
     * Builds the cache on first use. Later calls reuse it: the lifetime comes from the configuration
     * file, so it cannot change while the server runs.
     */
    private static void initialise(int ttlMinutes) {
        if (store != null) {
            return;
        }
        synchronized (GraphImportSessionCache.class) {
            if (store == null) {
                store = Caffeine.newBuilder()
                        .expireAfterAccess(Duration.ofMinutes(Math.max(1, ttlMinutes)))
                        .maximumSize(MAX_SESSIONS)
                        .build();
            }
        }
    }

    public GraphImportSession create(URI owner) {
        GraphImportSession session = new GraphImportSession(UUID.randomUUID().toString(), owner, Instant.now());
        store.put(session.getId(), session);
        return session;
    }

    /**
     * @return the import, only when it is open and belongs to {@code owner}. Someone else's import is
     * indistinguishable from one that does not exist.
     */
    public Optional<GraphImportSession> get(String id, URI owner) {
        GraphImportSession session = id == null ? null : store.getIfPresent(id);
        if (session == null || !session.isOwnedBy(owner)) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * Puts back an import read from the store.
     */
    public void put(GraphImportSession session) {
        store.put(session.getId(), session);
    }

    public void remove(String id) {
        if (id != null) {
            store.invalidate(id);
        }
    }
}
