//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.preview;

/**
 * What an import would do to a resource, once compared with the instance.
 *
 * @author Arnaud Charleroy
 */
public enum NodeState {
    /** Nothing in the instance answers to it: it would be created. */
    CREATED,
    /** It exists, and the file gives it values it does not hold yet. */
    COMPLETED,
    /** It exists and already holds every value the file gives. */
    UNCHANGED,
    /**
     * The file and the instance disagree: a value the instance holds would be replaced, the name
     * designates several resources, or the classes do not fit.
     */
    CONFLICT
}
