//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import java.net.URI;

/**
 * A resource that actually exists, either in this instance or in a shared resource instance.
 * <p>
 * Every URI shown to the user or handed to the assistant comes from one of these, read from a
 * database. None is ever produced by the language model.
 *
 * @author Arnaud Charleroy
 */
public class ResourceReference {

    private URI uri;
    private String name;

    /**
     * Identifier of the shared resource instance the resource was found on, or {@code null} when it
     * is local.
     */
    private String sharedResourceInstance;

    /**
     * Human label of that shared resource instance.
     */
    private String sharedResourceInstanceLabel;

    /**
     * For a variable, the datatype it expects. Needed to tell a column of decimals apart from a
     * variable declared as an integer.
     */
    private String datatype;

    public ResourceReference() {
    }

    public ResourceReference(URI uri, String name) {
        this.uri = uri;
        this.name = name;
    }

    public URI getUri() {
        return uri;
    }

    public ResourceReference setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getName() {
        return name;
    }

    public ResourceReference setName(String name) {
        this.name = name;
        return this;
    }

    public String getSharedResourceInstance() {
        return sharedResourceInstance;
    }

    public ResourceReference setSharedResourceInstance(String sharedResourceInstance) {
        this.sharedResourceInstance = sharedResourceInstance;
        return this;
    }

    public String getSharedResourceInstanceLabel() {
        return sharedResourceInstanceLabel;
    }

    public ResourceReference setSharedResourceInstanceLabel(String sharedResourceInstanceLabel) {
        this.sharedResourceInstanceLabel = sharedResourceInstanceLabel;
        return this;
    }

    public String getDatatype() {
        return datatype;
    }

    public ResourceReference setDatatype(String datatype) {
        this.datatype = datatype;
        return this;
    }
}
