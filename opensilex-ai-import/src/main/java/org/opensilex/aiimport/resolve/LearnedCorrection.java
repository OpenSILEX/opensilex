//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * A misspelling somebody confirmed once and asked the instance to remember.
 * <p>
 * It carries who and when because it acts on other people's imports: the next file that writes
 * "Chardonay" resolves to Chardonnay without asking, and the person importing it is entitled to
 * know on whose word — and to take it back if that word was wrong.
 *
 * @author Arnaud Charleroy
 */
public class LearnedCorrection {

    private final URI uri;
    private final ReportCategory category;
    private final String label;
    private final URI target;
    private final String targetName;
    private final String author;
    private final OffsetDateTime created;

    public LearnedCorrection(URI uri, ReportCategory category, String label, URI target,
                             String targetName, String author, OffsetDateTime created) {
        this.uri = uri;
        this.category = category;
        this.label = label;
        this.target = target;
        this.targetName = targetName;
        this.author = author;
        this.created = created;
    }

    /**
     * The correction itself, as a resource: what is deleted when it is forgotten.
     */
    public URI getUri() {
        return uri;
    }

    public ReportCategory getCategory() {
        return category;
    }

    /**
     * The misspelling, as the file wrote it when it was confirmed.
     */
    public String getLabel() {
        return label;
    }

    public URI getTarget() {
        return target;
    }

    /**
     * The resource's name when the correction was made. Shown only as a fallback: the current name
     * is read again, under the current user's rights, before the correction is applied.
     */
    public String getTargetName() {
        return targetName;
    }

    public String getAuthor() {
        return author;
    }

    public OffsetDateTime getCreated() {
        return created;
    }
}
