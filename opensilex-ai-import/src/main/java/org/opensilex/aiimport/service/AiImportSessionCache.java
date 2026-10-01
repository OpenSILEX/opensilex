//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jvnet.hk2.annotations.Service;
import org.opensilex.aiimport.AiImportConfig;
import org.opensilex.service.reflection.SelfBound;

import javax.inject.Inject;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Holds the open conversations in memory, for the lifetime configured on the module.
 * <p>
 * Same idea as the CSV validation cache of {@code opensilex-sparql}: a short-lived, bounded store
 * rather than a collection, so an abandoned upload disappears on its own. Nothing survives a
 * restart, which is acceptable while the module writes nothing.
 * <p>
 * The store is static because self-bound services are request scoped: a per-instance cache would be
 * discarded between two calls of the same conversation.
 *
 * @author Arnaud Charleroy
 */
@SelfBound
@Service
public class AiImportSessionCache {

    /**
     * Bounds how much a burst of uploads can claim. Each entry holds one workbook's structure.
     */
    private static final int MAX_SESSIONS = 200;

    private static final int DEFAULT_TTL_MINUTES = 60;

    private static volatile Cache<String, AiImportSession> store;

    @Inject
    public AiImportSessionCache(AiImportConfig config) {
        initialise(config == null ? DEFAULT_TTL_MINUTES : config.sessionTtlMinutes());
    }

    AiImportSessionCache(int ttlMinutes) {
        initialise(ttlMinutes);
    }

    /**
     * Builds the store on first use, with the configured lifetime. Later calls reuse it: the
     * lifetime comes from a configuration file, so it cannot change while the server runs.
     */
    private static void initialise(int ttlMinutes) {
        if (store != null) {
            return;
        }
        synchronized (AiImportSessionCache.class) {
            if (store == null) {
                store = Caffeine.newBuilder()
                        .expireAfterAccess(Duration.ofMinutes(Math.max(1, ttlMinutes)))
                        .maximumSize(MAX_SESSIONS)
                        .build();
            }
        }
    }

    public AiImportSession create(URI accountUri) {
        AiImportSession session = new AiImportSession(UUID.randomUUID().toString(), accountUri);
        store.put(session.getId(), session);
        return session;
    }

    /**
     * @return the session, only when it exists and belongs to {@code accountUri}. A conversation
     * opened by someone else is indistinguishable from one that never existed.
     */
    public Optional<AiImportSession> get(String id, URI accountUri) {
        AiImportSession session = id == null ? null : store.getIfPresent(id);
        if (session == null || !session.isOwnedBy(accountUri)) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * Puts back a session read from storage, under its own identifier.
     */
    public void put(AiImportSession session) {
        store.put(session.getId(), session);
    }

    public void remove(String id) {
        if (id != null) {
            store.invalidate(id);
        }
    }
}
