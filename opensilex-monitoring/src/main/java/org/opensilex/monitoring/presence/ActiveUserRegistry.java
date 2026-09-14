//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.presence;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window of accounts that made a call recently.
 *
 * <p>This is deliberately a different measurement from the authentication token registry. That one
 * answers "who holds a session"; this one answers "who is doing something". A tab left open on a
 * dashboard is connected but not active; a scheduled job with a long-lived token is active with
 * nobody at a keyboard. Both numbers are worth showing, and this one has the advantage of being
 * the exact same measurement as the per-bucket user count in the activity chart, so the live
 * figure and the historical series cannot disagree.</p>
 *
 * @author Arnaud Charleroy
 */
public class ActiveUserRegistry {

    /** Pruning is lazy, but never let the map grow past this without a sweep. */
    private static final int PRUNE_THRESHOLD = 2048;

    private final ConcurrentHashMap<URI, Long> lastSeen = new ConcurrentHashMap<>();

    public void touch(URI account) {
        if (account == null) {
            return;
        }
        lastSeen.put(account, System.currentTimeMillis());
        if (lastSeen.size() > PRUNE_THRESHOLD) {
            prune(System.currentTimeMillis() - java.time.Duration.ofDays(1).toMillis());
        }
    }

    /**
     * @param windowMinutes how far back an account still counts as active
     * @return number of distinct accounts seen inside the window
     */
    public int countActive(int windowMinutes) {
        long floor = System.currentTimeMillis() - (long) windowMinutes * 60_000L;
        prune(floor);
        return lastSeen.size();
    }

    private void prune(long floor) {
        lastSeen.entrySet().removeIf((Map.Entry<URI, Long> entry) -> entry.getValue() < floor);
    }

    public void clear() {
        lastSeen.clear();
    }
}
