//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * What the rows of a sheet are, as the user describes it. The one place that says what a kind
 * means: the profile, the rules that check a description and the interface that lists the choices
 * all read it, so a new kind is one constant here and two translations.
 *
 * @author Arnaud Charleroy
 */
public enum SheetKind {

    /**
     * A row is a scientific object.
     */
    OBJECTS(true, false, Oeso.ScientificObject.getURI()),

    /**
     * A row is an observation of an object, or of a facility, that the file or the instance
     * already knows.
     */
    DATA(false, true, null),

    /**
     * A row is an observation, and the sheet also describes its objects: they are the distinct
     * values of its identifier column.
     */
    OBJECTS_AND_DATA(true, true, Oeso.ScientificObject.getURI()),

    /**
     * A row is a level of a factor of the experiment.
     */
    FACTORS(false, false, null),

    /**
     * A row is a facility.
     */
    FACILITIES(false, false, Oeso.Facility.getURI()),

    /**
     * A row is an experiment: its name, and what the form that creates it asks — objective,
     * description, dates, the projects it belongs to, the facilities it uses.
     */
    EXPERIMENTS(false, false, null),

    /**
     * A row is an event that happened to an object or at a facility — a spraying, an incident, an
     * irrigation — of a type the user chooses, with the properties that type has.
     */
    EVENTS(false, false, Oeev.Event.getURI()),

    /**
     * The sheet is not read.
     */
    IGNORED(false, false, null);

    public static final String LABEL_KEY_PREFIX = "AiImport.describe.kind_";

    private final boolean describesObjects;
    private final boolean holdsData;
    private final URI baseType;

    SheetKind(boolean describesObjects, boolean holdsData, String baseType) {
        this.describesObjects = describesObjects;
        this.holdsData = holdsData;
        this.baseType = baseType == null ? null : URI.create(baseType);
    }

    /**
     * @return whether the sheet names scientific objects
     */
    public boolean describesObjects() {
        return describesObjects;
    }

    /**
     * @return whether the sheet holds observations
     */
    public boolean holdsData() {
        return holdsData;
    }

    /**
     * @return whether the sheet is read at all
     */
    public boolean isRead() {
        return this != IGNORED;
    }

    /**
     * @return the class the types of the rows descend from, when the rows have a type — the one the
     * ontology is asked for the properties of that type
     */
    public Optional<URI> baseType() {
        return Optional.ofNullable(baseType);
    }

    /**
     * @return the translation key of the kind's label
     */
    public String labelKey() {
        return LABEL_KEY_PREFIX + name();
    }

    /**
     * @return the kind a request names, or empty when it names none
     */
    public static Optional<SheetKind> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
