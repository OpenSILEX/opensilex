//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import org.opensilex.aiimport.report.ReportMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * One name read from the file, and what the instance knows about it.
 *
 * @author Arnaud Charleroy
 */
public class ResolvedItem {

    /**
     * The value as written in the file.
     */
    private String sourceValue;

    /**
     * An external identifier read from the file, when it provides one, e.g. a CropOntology id.
     */
    private String externalId;

    private ResolutionStatus status = ResolutionStatus.NOT_CHECKED;

    private List<ResourceReference> matches = new ArrayList<>();

    /**
     * What the user should do about this item, in one sentence.
     */
    private ReportMessage hint;

    /**
     * For a variable: the parts it is made of, as the file names them and as this instance answers.
     * Empty for everything else, and for a file that does not describe its variables.
     */
    private final List<ResolvedComponent> components = new ArrayList<>();

    public ResolvedItem() {
    }

    public ResolvedItem(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public ResolvedItem setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
        return this;
    }

    public String getExternalId() {
        return externalId;
    }

    public ResolvedItem setExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public ResolutionStatus getStatus() {
        return status;
    }

    public ResolvedItem setStatus(ResolutionStatus status) {
        this.status = status;
        return this;
    }

    public List<ResourceReference> getMatches() {
        return matches;
    }

    public ResolvedItem setMatches(List<ResourceReference> matches) {
        this.matches = matches;
        return this;
    }

    public String getHint() {
        return hint == null ? null : hint.getEnglish();
    }

    /**
     * @return the hint as a translation key and its parameters, for the interface
     */
    public List<ResolvedComponent> getComponents() {
        return components;
    }

    public ReportMessage getHintMessage() {
        return hint;
    }

    public ResolvedItem setHint(ReportMessage hint) {
        this.hint = hint;
        return this;
    }

    /**
     * A hint with no translation yet. It still reaches the interface, in English, rather than
     * being dropped for want of a key.
     */
    public ResolvedItem setHint(String hint) {
        this.hint = hint == null ? null : ReportMessage.plain(hint);
        return this;
    }
}
