//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.mapping;

/**
 * What a column of the file stands for, in OpenSILEX terms.
 * <p>
 * Decided by the profile, which knows the template, and never by the language model: the mapping is
 * what everything else is built on, so it has to be reproducible.
 *
 * @author Arnaud Charleroy
 */
public enum ColumnRole {

    /**
     * Names the observed object, e.g. a unit plot. Becomes a scientific object.
     */
    OBJECT("ScientificObject", "Identifies the observed object"),

    /**
     * Names the trial. Becomes an experiment.
     */
    TRIAL("Experiment", "Names the trial"),

    /**
     * Names the project the trial belongs to.
     */
    PROJECT("Project", "Names the project"),

    /**
     * Names the plant material. Becomes a germplasm.
     */
    GERMPLASM("Germplasm", "Names the plant material"),

    /**
     * A treatment or group label, e.g. treated versus untreated control.
     */
    FACTOR_LEVEL("FactorLevel", "Treatment or group label"),

    /**
     * Locates the object in the field. Belongs on the scientific object, not on the data.
     */
    LOCATION("ScientificObject", "Locates the object in the field"),

    /**
     * The x/y position of the object, which OpenSILEX records as a <b>move event</b> rather than as
     * an attribute — a position is dated, because a pot moves and a plot does not.
     * <p>
     * The scientific object CSV import carries exactly these columns
     * ({@code x}, {@code y}, {@code z}, {@code textualPosition}, with a start and an end date) and
     * turns them into a {@code MoveModel}. Writing them as properties instead would put a
     * coordinate on the object with no date and no history.
     */
    POSITION("Move", "Positions the object, through a move event"),

    /**
     * Names a group the observed objects belong to — a block, in a randomised design.
     * <p>
     * Two readings are legitimate and OpenSILEX supports both: the block is a scientific object of
     * its own, with the plots as its parts, or it is a property carried by each plot. Which one is
     * right depends on whether anything is ever observed on the block itself, and only the user
     * knows that. This role says what the column is; it does not settle the modelling.
     */
    PARENT_OBJECT("ScientificObject", "Names a group the objects belong to, such as a block"),

    /**
     * Describes the observed object rather than locating it or measuring it: the first row of a
     * unit plot, its spacing, the number of vines it holds.
     * <p>
     * The distinction matters at creation time. A property is written once, on the scientific
     * object, as a relation of the ontology; a measurement is written many times, as data with a
     * date and a provenance. Filing one as the other produces either a variable nobody ever
     * observes, or an object attribute buried in a time series.
     */
    OBJECT_PROPERTY("ScientificObject", "Describes the observed object"),

    /**
     * The season or campaign year.
     */
    SEASON("Experiment", "Season or campaign year"),

    /**
     * Dates the observation.
     */
    DATE("Data", "Dates the observation"),

    /**
     * Identifies who observed. Belongs on the data provenance.
     */
    OBSERVER("Provenance", "Identifies who observed"),

    /**
     * Someone named as a person rather than as an observer code: a study contact, an author. They
     * become an OpenSILEX Person, which carries an email, an ORCID and an affiliation.
     */
    PERSON("Person", "Names a person: a contact, an author"),

    /**
     * Holds a measurement. Becomes a variable, and its cells become data.
     */
    VARIABLE("Variable", "Holds a measurement"),

    /**
     * Free text, kept as a note rather than measured.
     */
    COMMENT(null, "Free comment, not a measurement"),

    /**
     * The profile could not say. The user has to.
     */
    UNKNOWN(null, "Not recognised, ask the user");

    private final String entity;
    private final String explanation;

    ColumnRole(String entity, String explanation) {
        this.entity = entity;
        this.explanation = explanation;
    }

    /**
     * @return the OpenSILEX concept this column feeds, or {@code null} when it feeds none
     */
    public String getEntity() {
        return entity;
    }

    public String getExplanation() {
        return explanation;
    }
}
