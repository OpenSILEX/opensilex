//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

/**
 * What the user can create from an open conversation.
 *
 * @author Arnaud Charleroy
 */
public enum CreationTarget {

    PROJECT,
    EXPERIMENT,

    /**
     * Variables brought in from a shared resource instance, their components with them. Creating a
     * variable from nothing is a different target: this one only copies what already exists
     * somewhere authoritative.
     */
    VARIABLE,

    /**
     * What happened during the trial: sprayings, incidents, observation rounds.
     */
    EVENT,

    DATA
}
