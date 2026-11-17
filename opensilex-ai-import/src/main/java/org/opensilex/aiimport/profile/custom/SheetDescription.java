//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the user said of one sheet — and only that. What was left unsaid is not here: the defaults
 * are worked out when the sheet is read, never stored, so that a better default tomorrow reaches
 * the descriptions already made.
 *
 * @author Arnaud Charleroy
 */
public class SheetDescription {

    private final String sheet;
    private SheetKind kind;
    private final Map<String, DescribedRole> roles = new LinkedHashMap<>();
    private URI type;
    private final Map<String, String> properties = new LinkedHashMap<>();
    private String factor;

    public SheetDescription(String sheet) {
        this.sheet = sheet;
    }

    public String getSheet() {
        return sheet;
    }

    /**
     * @return a copy to work on without changing this one: a change is checked on the copy, and
     * only kept when everything it asks for holds
     */
    public SheetDescription copy() {
        SheetDescription copy = new SheetDescription(sheet).setKind(kind).setType(type).setFactor(factor);
        copy.roles.putAll(roles);
        copy.properties.putAll(properties);
        return copy;
    }

    /**
     * @return the kind the user chose, or {@code null} when they chose none
     */
    public SheetKind getKind() {
        return kind;
    }

    public SheetDescription setKind(SheetKind kind) {
        this.kind = kind;
        return this;
    }

    /**
     * @return the role the user chose for each column they chose one for, by header
     */
    public Map<String, DescribedRole> getRoles() {
        return roles;
    }

    /**
     * @return the type the user chose for the rows of the sheet — of objects or of facilities —
     * or {@code null}
     */
    public URI getType() {
        return type;
    }

    public SheetDescription setType(URI type) {
        this.type = type;
        return this;
    }

    /**
     * @return the property of the type each column described as one is written to, by header, as
     * the URI the ontology gives it
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @return the name of the factor whose levels a sheet of levels lists when no column says it, or
     * {@code null}: every row of such a sheet is then a level of this one factor
     */
    public String getFactor() {
        return factor;
    }

    public SheetDescription setFactor(String factor) {
        this.factor = factor == null || factor.isBlank() ? null : factor.trim();
        return this;
    }
}
