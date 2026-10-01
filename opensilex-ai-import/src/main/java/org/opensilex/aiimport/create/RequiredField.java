//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

/**
 * A field that has to be filled before a resource can be created, and the value the file suggests
 * for it.
 * <p>
 * A suggestion is only ever a default: it is shown in the form and the user can change it, because
 * a name read out of a spreadsheet is a guess about intent, not a decision.
 *
 * @author Arnaud Charleroy
 */
public class RequiredField {

    private String name;

    /**
     * Translation key for the label shown in the form.
     */
    private String labelKey;

    /**
     * text, date, or uri-list.
     */
    private String kind;

    private boolean required;

    /**
     * Which referential the field designates, when it designates one: {@code experiment},
     * {@code project}, {@code entity}, {@code characteristic}, {@code method}, {@code unit}.
     * <p>
     * It exists so the interface can hand the field to the selector OpenSILEX already has for that
     * resource — one that searches, paginates and shows what exists — instead of asking the user
     * to paste a URI into a text box. Null for a plain value.
     */
    private String resource;

    /**
     * What the file suggests, or {@code null} when it suggests nothing.
     */
    private String suggestedValue;

    /**
     * Where the suggestion came from, so the user can judge it.
     */
    private String suggestedFrom;

    public RequiredField() {
    }

    public RequiredField(String name, String labelKey, String kind, boolean required) {
        this.name = name;
        this.labelKey = labelKey;
        this.kind = kind;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public RequiredField setName(String name) {
        this.name = name;
        return this;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public RequiredField setLabelKey(String labelKey) {
        this.labelKey = labelKey;
        return this;
    }

    public String getResource() {
        return resource;
    }

    public RequiredField setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public String getKind() {
        return kind;
    }

    public RequiredField setKind(String kind) {
        this.kind = kind;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public RequiredField setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public String getSuggestedValue() {
        return suggestedValue;
    }

    public RequiredField setSuggestedValue(String suggestedValue) {
        this.suggestedValue = suggestedValue;
        return this;
    }

    public String getSuggestedFrom() {
        return suggestedFrom;
    }

    public RequiredField setSuggestedFrom(String suggestedFrom) {
        this.suggestedFrom = suggestedFrom;
        return this;
    }

    /**
     * The kind of a field that is a decision rather than a value: rendered as a checkbox, and
     * satisfied only when it is ticked.
     */
    public static final String KIND_BOOLEAN = "boolean";

    public static final String KIND_TEXT = "text";
    public static final String KIND_LONG_TEXT = "textarea";
    public static final String KIND_DATE = "date";
    public static final String KIND_URI = "uri";
    public static final String KIND_URI_LIST = "uri-list";

    public static final String RESOURCE_EXPERIMENT = "experiment";
    public static final String RESOURCE_PROJECT = "project";
    public static final String RESOURCE_OBJECT_TYPE = "objectType";
    public static final String RESOURCE_ORGANIZATION = "organization";
    public static final String RESOURCE_FACILITY = "facility";

    /**
     * Whether a submitted value satisfies this field.
     * <p>
     * Asked here rather than in the API because the answer depends on the kind: any non-blank text
     * fills a text field, but an unticked checkbox is a decision not taken, and "false" is exactly
     * what an unticked checkbox submits.
     */
    public boolean isSatisfiedBy(String value) {
        if (!required) {
            return true;
        }
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return !KIND_BOOLEAN.equals(kind) || Boolean.parseBoolean(value.trim());
    }

    public RequiredField suggest(String value, String from) {
        this.suggestedValue = value;
        this.suggestedFrom = from;
        return this;
    }
}
