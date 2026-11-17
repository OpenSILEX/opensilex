//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

/**
 * The field of a platform form a role of a column feeds: the entity the form creates and the
 * name of the field. The roles that feed one carry it in {@link DescribedRole}, so that what a
 * column becomes in a form is read from the same table as everything else about a role.
 *
 * @author Arnaud Charleroy
 */
public enum FormTarget {

    EXPERIMENT_ALTERNATIVE_NAME(Entity.EXPERIMENT, "alternative_name", false, false),
    EXPERIMENT_DESCRIPTION(Entity.EXPERIMENT, "description", true, false),
    EXPERIMENT_OBJECTIVE(Entity.EXPERIMENT, "objective", true, false),
    EXPERIMENT_START(Entity.EXPERIMENT, "start_date", false, true),
    EXPERIMENT_END(Entity.EXPERIMENT, "end_date", false, true),
    PROJECT_SHORTNAME(Entity.PROJECT, "shortname", false, false),
    PROJECT_DESCRIPTION(Entity.PROJECT, "description", true, false),
    PROJECT_OBJECTIVE(Entity.PROJECT, "objective", true, false),
    PROJECT_START(Entity.PROJECT, "start_date", false, true),
    PROJECT_END(Entity.PROJECT, "end_date", false, true);

    /**
     * What a form creates, and the role of the column that names it: the other columns of the same
     * row say more of that entity.
     */
    public enum Entity {
        EXPERIMENT(DescribedRole.TRIAL),
        PROJECT(DescribedRole.PROJECT);

        private final DescribedRole nameRole;

        Entity(DescribedRole nameRole) {
            this.nameRole = nameRole;
        }

        public DescribedRole nameRole() {
            return nameRole;
        }
    }

    private final Entity entity;
    private final String field;
    private final boolean concatenated;
    private final boolean date;

    FormTarget(Entity entity, String field, boolean concatenated, boolean date) {
        this.entity = entity;
        this.field = field;
        this.concatenated = concatenated;
        this.date = date;
    }

    public Entity entity() {
        return entity;
    }

    /**
     * @return the name of the field, as the requirements of the creation call it
     */
    public String field() {
        return field;
    }

    /**
     * @return whether several columns, and several rows, add up in the field rather than the first
     * value standing
     */
    public boolean isConcatenated() {
        return concatenated;
    }

    /**
     * @return whether the field holds a date, written as {@code yyyy-MM-dd}
     */
    public boolean isDate() {
        return date;
    }
}
