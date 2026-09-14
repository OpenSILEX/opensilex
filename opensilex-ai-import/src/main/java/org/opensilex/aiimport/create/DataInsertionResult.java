//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import java.util.ArrayList;
import java.util.List;

/**
 * The outcome of inserting the observations: either everything was written, or nothing was.
 * <p>
 * All or nothing on purpose. Writing the rows that happen to resolve and dropping the rest leaves
 * the user believing they imported their dataset when they imported part of it, and nothing on
 * screen says which part. A refusal that names the offending rows is recoverable; a silent partial
 * import is not.
 *
 * @author Arnaud Charleroy
 */
public class DataInsertionResult {

    private final int insertedCount;
    private final List<UnresolvedRow> unresolved = new ArrayList<>();

    /**
     * How many rows could not be resolved in all, which may exceed the sample kept in
     * {@link #getUnresolved()}.
     */
    private final int unresolvedCount;

    private DataInsertionResult(int insertedCount, List<UnresolvedRow> unresolved,
                                int unresolvedCount) {
        this.insertedCount = insertedCount;
        this.unresolved.addAll(unresolved);
        this.unresolvedCount = unresolvedCount;
    }

    public static DataInsertionResult inserted(int count) {
        return new DataInsertionResult(count, List.of(), 0);
    }

    /**
     * @param sample  the first few offending rows, enough to see the pattern
     * @param total   how many there are in all
     */
    public static DataInsertionResult refused(List<UnresolvedRow> sample, int total) {
        return new DataInsertionResult(0, sample, total);
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public List<UnresolvedRow> getUnresolved() {
        return unresolved;
    }

    public int getUnresolvedCount() {
        return unresolvedCount;
    }

    public boolean isRefused() {
        return unresolvedCount > 0;
    }
}
