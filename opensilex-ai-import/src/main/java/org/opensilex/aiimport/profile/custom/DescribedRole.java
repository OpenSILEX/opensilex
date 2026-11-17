//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import org.opensilex.aiimport.mapping.ColumnRole;
import org.opensilex.aiimport.profile.ObjectTargets;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.opensilex.aiimport.profile.custom.SheetKind.DATA;
import static org.opensilex.aiimport.profile.custom.SheetKind.EVENTS;
import static org.opensilex.aiimport.profile.custom.SheetKind.EXPERIMENTS;
import static org.opensilex.aiimport.profile.custom.SheetKind.FACILITIES;
import static org.opensilex.aiimport.profile.custom.SheetKind.FACTORS;
import static org.opensilex.aiimport.profile.custom.SheetKind.OBJECTS;
import static org.opensilex.aiimport.profile.custom.SheetKind.OBJECTS_AND_DATA;

/**
 * What a column can be described as, and everything that follows from it: the table every other
 * class of this package reads. A new role is an entry here and two translations; nothing else
 * switches on the role.
 *
 * @author Arnaud Charleroy
 */
public enum DescribedRole {

    UNREAD(ColumnRole.UNKNOWN, kinds(), kinds(), false, ObjectTargets.NONE),
    OBJECT(ColumnRole.OBJECT, kinds(OBJECTS, DATA, OBJECTS_AND_DATA, EVENTS), kinds(OBJECTS, OBJECTS_AND_DATA), true,
            ObjectTargets.NAME),
    DATE(ColumnRole.DATE, kinds(DATA, OBJECTS_AND_DATA, EVENTS), kinds(DATA, OBJECTS_AND_DATA, EVENTS), true,
            ObjectTargets.NONE),
    VARIABLE(ColumnRole.VARIABLE, kinds(DATA, OBJECTS_AND_DATA), kinds(DATA, OBJECTS_AND_DATA), false,
            ObjectTargets.NONE),
    GERMPLASM(ColumnRole.GERMPLASM, kinds(OBJECTS, OBJECTS_AND_DATA), kinds(), false, ObjectTargets.GERMPLASM),
    FACTOR_LEVEL(ColumnRole.FACTOR_LEVEL, kinds(OBJECTS, OBJECTS_AND_DATA, FACTORS), kinds(FACTORS), false,
            ObjectTargets.FACTOR_LEVEL),

    /**
     * Names the factor a level belongs to: the column of a sheet of levels, where the header of a
     * level column of an object sheet is itself the name of its factor.
     */
    FACTOR(ColumnRole.FACTOR_LEVEL, kinds(FACTORS), kinds(FACTORS), true, ObjectTargets.NONE),

    /**
     * What a sheet of levels says of a level besides its code — the code stays the level's name,
     * since it is what the objects carry — and of the factor: the factor form is opened with them.
     */
    FACTOR_LEVEL_NAME(ColumnRole.UNKNOWN, kinds(FACTORS), kinds(), true, ObjectTargets.NONE),
    FACTOR_LEVEL_DESCRIPTION(ColumnRole.UNKNOWN, kinds(FACTORS), kinds(), false, ObjectTargets.NONE),
    FACTOR_DESCRIPTION(ColumnRole.UNKNOWN, kinds(FACTORS), kinds(), false, ObjectTargets.NONE),
    PARENT_OBJECT(ColumnRole.PARENT_OBJECT, kinds(OBJECTS, OBJECTS_AND_DATA), kinds(), false, ObjectTargets.PARENT),

    /**
     * The target of a position column is its axis, which only the header says.
     */
    POSITION(ColumnRole.POSITION, kinds(OBJECTS, OBJECTS_AND_DATA), kinds(), false, ObjectTargets.NONE),
    OBJECT_PROPERTY(ColumnRole.OBJECT_PROPERTY, kinds(OBJECTS, OBJECTS_AND_DATA, FACILITIES, EVENTS), kinds(), false,
            ObjectTargets.NONE),

    /**
     * The name of a facility: the one that hosts the object of the row, the one a sheet of weather
     * data measures at, or the facility a row of a sheet of facilities is.
     */
    LOCATION(ColumnRole.LOCATION, kinds(OBJECTS, DATA, OBJECTS_AND_DATA, FACILITIES, EVENTS, EXPERIMENTS), kinds(FACILITIES),
            true,
            ObjectTargets.NONE),
    TRIAL(ColumnRole.TRIAL, readKinds(), kinds(EXPERIMENTS), false, ObjectTargets.NONE),
    PROJECT(ColumnRole.PROJECT, readKinds(), kinds(), false, ObjectTargets.NONE),
    PERSON(ColumnRole.PERSON, readKinds(), kinds(), false, ObjectTargets.NONE),
    OBSERVER(ColumnRole.OBSERVER, kinds(DATA, OBJECTS_AND_DATA), kinds(), false, ObjectTargets.NONE),
    COMMENT(ColumnRole.COMMENT, readKinds(), kinds(), false, ObjectTargets.NONE),

    /**
     * What the other columns of the row say of the experiment the row's trial column names, or of the
     * project its project column names, for the form that creates it. They are not read for the sheet
     * itself: a row without that name feeds nothing.
     */
    EXPERIMENT_ALTERNATIVE_NAME(readKinds(), FormTarget.EXPERIMENT_ALTERNATIVE_NAME),
    EXPERIMENT_DESCRIPTION(readKinds(), FormTarget.EXPERIMENT_DESCRIPTION),
    EXPERIMENT_OBJECTIVE(readKinds(), FormTarget.EXPERIMENT_OBJECTIVE),
    EXPERIMENT_START(readKinds(), FormTarget.EXPERIMENT_START),
    EXPERIMENT_END(readKinds(), FormTarget.EXPERIMENT_END),
    PROJECT_SHORTNAME(readKinds(), FormTarget.PROJECT_SHORTNAME),
    PROJECT_DESCRIPTION(readKinds(), FormTarget.PROJECT_DESCRIPTION),
    PROJECT_OBJECTIVE(readKinds(), FormTarget.PROJECT_OBJECTIVE),
    PROJECT_START(readKinds(), FormTarget.PROJECT_START),
    PROJECT_END(readKinds(), FormTarget.PROJECT_END);

    public static final String LABEL_KEY_PREFIX = "AiImport.describe.role_";

    private final ColumnRole columnRole;
    private final Set<SheetKind> acceptedBy;
    private final Set<SheetKind> requiredBy;
    private final boolean singlePerSheet;
    private final String defaultObjectTarget;
    private final FormTarget formTarget;

    DescribedRole(Set<SheetKind> acceptedBy, FormTarget formTarget) {
        this(ColumnRole.UNKNOWN, acceptedBy, kinds(), false, ObjectTargets.NONE, formTarget);
    }

    DescribedRole(ColumnRole columnRole, Set<SheetKind> acceptedBy, Set<SheetKind> requiredBy,
                  boolean singlePerSheet, String defaultObjectTarget) {
        this(columnRole, acceptedBy, requiredBy, singlePerSheet, defaultObjectTarget, null);
    }

    DescribedRole(ColumnRole columnRole, Set<SheetKind> acceptedBy, Set<SheetKind> requiredBy,
                  boolean singlePerSheet, String defaultObjectTarget, FormTarget formTarget) {
        this.formTarget = formTarget;
        this.columnRole = columnRole;
        this.acceptedBy = acceptedBy;
        this.requiredBy = requiredBy;
        this.singlePerSheet = singlePerSheet;
        this.defaultObjectTarget = defaultObjectTarget;
    }

    /**
     * @return what the platform's column mapping calls it
     */
    public ColumnRole columnRole() {
        return columnRole;
    }

    /**
     * @return whether a sheet of this kind may have a column described so. Everything is accepted
     * by every kind that reads, except where the role says otherwise.
     */
    public boolean isAcceptedBy(SheetKind kind) {
        return this == UNREAD ? true : acceptedBy.contains(kind);
    }

    /**
     * @return whether a sheet of this kind cannot be read without such a column
     */
    public boolean isRequiredBy(SheetKind kind) {
        return requiredBy.contains(kind);
    }

    /**
     * @return whether a sheet has at most one column described so
     */
    public boolean isSinglePerSheet() {
        return singlePerSheet;
    }

    /**
     * @return what a column of this role becomes in the object sheets panel, when the role alone
     * settles it; position and comment depend on the header and the kind
     */
    public String defaultObjectTarget() {
        return defaultObjectTarget;
    }

    /**
     * @return the field of a form this role feeds, when it feeds one
     */
    public Optional<FormTarget> formTarget() {
        return Optional.ofNullable(formTarget);
    }

    public String labelKey() {
        return LABEL_KEY_PREFIX + name();
    }

    /**
     * @return the role the platform's header dictionary gave, in these terms; empty when the
     * dictionary did not recognise the header or answered something this tool does not describe
     */
    public static Optional<DescribedRole> from(ColumnRole role) {
        switch (role) {
            case OBJECT:
                return Optional.of(OBJECT);
            case TRIAL:
                return Optional.of(TRIAL);
            case PROJECT:
                return Optional.of(PROJECT);
            case GERMPLASM:
                return Optional.of(GERMPLASM);
            case FACTOR_LEVEL:
                return Optional.of(FACTOR_LEVEL);
            case LOCATION:
                return Optional.of(LOCATION);
            case POSITION:
                return Optional.of(POSITION);
            case PARENT_OBJECT:
                return Optional.of(PARENT_OBJECT);
            case OBJECT_PROPERTY:
                return Optional.of(OBJECT_PROPERTY);
            case DATE:
                return Optional.of(DATE);
            case OBSERVER:
                return Optional.of(OBSERVER);
            case PERSON:
                return Optional.of(PERSON);
            case VARIABLE:
                return Optional.of(VARIABLE);
            case COMMENT:
                return Optional.of(COMMENT);
            default:
                return Optional.empty();
        }
    }

    public static Optional<DescribedRole> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Set<SheetKind> kinds(SheetKind... kinds) {
        return kinds.length == 0 ? EnumSet.noneOf(SheetKind.class) : EnumSet.copyOf(Arrays.asList(kinds));
    }

    private static Set<SheetKind> readKinds() {
        Set<SheetKind> kinds = EnumSet.allOf(SheetKind.class);
        kinds.remove(SheetKind.IGNORED);
        return kinds;
    }
}
