//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.steps;

/**
 * Where a step of the import stands.
 *
 * @author Arnaud Charleroy
 */
public enum StepStatus {

    /**
     * Everything the files name for this step exists on the instance.
     */
    DONE,

    /**
     * Something is still to be declared, and nothing it depends on is in the way.
     */
    POSSIBLE,

    /**
     * Something is still to be declared, but a step it depends on is not done.
     */
    BLOCKED,

    /**
     * The files name nothing for this step.
     */
    NOT_APPLICABLE
}
