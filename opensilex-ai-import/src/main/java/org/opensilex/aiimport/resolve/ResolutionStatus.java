//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

/**
 * Outcome of looking one name from the file up in the instance.
 *
 * @author Arnaud Charleroy
 */
public enum ResolutionStatus {

    /**
     * Exactly one resource matches the name.
     */
    FOUND,

    /**
     * Several resources match, so a human has to pick.
     */
    AMBIGUOUS,

    /**
     * Nothing matches locally, but a shared resource instance has it.
     */
    FOUND_IN_SHARED_RESOURCE,

    /**
     * Nothing matches. Something has to be created before the data can be imported.
     */
    MISSING,

    /**
     * The lookup could not be performed, for instance because it depends on an experiment that is
     * itself missing.
     */
    NOT_CHECKED
}
