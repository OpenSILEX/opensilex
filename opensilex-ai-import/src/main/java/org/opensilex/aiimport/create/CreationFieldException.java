//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

/**
 * A creation refused because of one identified field, rather than because of something the user
 * cannot see.
 * <p>
 * It exists to name the field. Without it, an experiment linked to a project URI that does not
 * resolve fails deep inside the SPARQL layer, and what reaches the interface is
 * {@code SPARQLInvalidUriListException} with a list of URIs and no indication of which form field
 * to go and fix — a message about the storage, where the user needs a message about their form.
 *
 * @author Arnaud Charleroy
 */
public class CreationFieldException extends Exception {

    private final String field;

    public CreationFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * @return the name of the field to correct, as the requirements name it
     */
    public String getField() {
        return field;
    }
}
