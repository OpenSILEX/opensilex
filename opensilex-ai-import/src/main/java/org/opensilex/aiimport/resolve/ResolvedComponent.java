//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import java.net.URI;

/**
 * One of the four parts a variable is made of, as the file names it and as this instance answers.
 * <p>
 * A missing variable is not one problem but up to five: the variable, and each of the entity,
 * characteristic, method and unit it needs. Saying which of the four already exist here turns
 * "create this variable" from a blank form into two or three choices — and stops a component being
 * created twice under two spellings, which no later cleanup undoes.
 *
 * @author Arnaud Charleroy
 */
public class ResolvedComponent {

    /**
     * Which part this is: {@code entity}, {@code characteristic}, {@code method} or {@code unit}.
     * A plain string because it crosses to the interface, where it names the selector to fill.
     */
    private final String role;

    /**
     * The name the file gives, which is what the user will recognise.
     */
    private final String name;

    /**
     * An ontology accession when the file gives one.
     */
    private final String accession;

    /**
     * Where it is here, when it is here at all. Null means the user has to choose or create it.
     */
    private URI uri;

    /**
     * When nothing here carries the file's name exactly: the closest existing one, offered for the
     * user to pick in the variable form — never selected on their behalf.
     */
    private ResourceReference suggestion;

    public ResolvedComponent(String role, String name, String accession) {
        this.role = role;
        this.name = name;
        this.accession = accession;
    }

    public String getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getAccession() {
        return accession;
    }

    public URI getUri() {
        return uri;
    }

    public ResolvedComponent setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public ResourceReference getSuggestion() {
        return suggestion;
    }

    public ResolvedComponent setSuggestion(ResourceReference suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public boolean isFound() {
        return uri != null;
    }
}
