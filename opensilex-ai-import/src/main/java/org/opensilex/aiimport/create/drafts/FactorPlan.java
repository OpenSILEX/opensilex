//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.drafts;

import java.net.URI;
import java.util.List;

/**
 * One factor of the experiment as the files have it, against what the experiment already holds.
 * An experiment can have several factors, so each is its own plan: the screen declares them one
 * at a time, with the platform's own factor form.
 *
 * @param name        the factor's name, or {@code null} when the file does not name it
 * @param uri         the factor on the instance, or {@code null} when the experiment has none of that name
 * @param description what the instance says of it when it exists, else what the files say of it
 * @param levels      the levels the form starts with: those the factor already has, then those the files add
 * @param status      what is left to do
 * @author Arnaud Charleroy
 */
public record FactorPlan(String name, URI uri, String description, List<Level> levels, Status status) {

    /**
     * @param uri    the level on the instance, or {@code null} for a level still to be created
     * @param exists whether the factor already has it
     */
    public record Level(URI uri, String name, String description, boolean exists) {
    }

    public enum Status {

        /**
         * The experiment has no such factor: it is created with its levels.
         */
        NEW,

        /**
         * The factor exists and lacks levels the files name: it is edited, with all its levels, because
         * the platform adds a level by updating its factor.
         */
        INCOMPLETE,

        /**
         * Nothing is missing.
         */
        COMPLETE
    }
}
