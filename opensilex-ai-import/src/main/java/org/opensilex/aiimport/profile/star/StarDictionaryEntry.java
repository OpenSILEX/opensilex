//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.star;

/**
 * One line of a STAR dictionary: what a column of the workbook means.
 *
 * @author Arnaud Charleroy
 */
public class StarDictionaryEntry {

    /**
     * The column name this line describes, e.g. {@code PM_BER_PC} or {@code plot_id}.
     */
    private String name;

    private String description;

    /**
     * Unit of measurement, when the column has one.
     */
    private String unit;

    /**
     * The type the file declares, in R's vocabulary: {@code numeric}, {@code character},
     * {@code date}, or the misspelt {@code interger}.
     */
    private String rClass;

    /**
     * Ontology URI, either a full CropOntology URL or a prefixed local term such as
     * {@code vignevin:bbch_stage}.
     */
    private String uri;

    /**
     * Present only in the revision that keeps variables and metadata in one sheet.
     */
    private Boolean variable;

    /**
     * The ELOA class this column belongs to, e.g. {@code Variable} or
     * {@code Traitement expérimental}.
     */
    private String eloaClass;

    public String getName() {
        return name;
    }

    public StarDictionaryEntry setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public StarDictionaryEntry setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getUnit() {
        return unit;
    }

    public StarDictionaryEntry setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    public String getRClass() {
        return rClass;
    }

    public StarDictionaryEntry setRClass(String rClass) {
        this.rClass = rClass;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public StarDictionaryEntry setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Boolean getVariable() {
        return variable;
    }

    public StarDictionaryEntry setVariable(Boolean variable) {
        this.variable = variable;
        return this;
    }

    public String getEloaClass() {
        return eloaClass;
    }

    public StarDictionaryEntry setEloaClass(String eloaClass) {
        this.eloaClass = eloaClass;
        return this;
    }

    /**
     * @return true when this column carries a measurement.
     * <p>
     * The split revision has no {@code is_variable} column — being in the variables sheet is what
     * says so — hence the flag passed in rather than read here.
     */
    public boolean isVariable(boolean fromVariablesSheet) {
        if (variable != null) {
            return variable;
        }
        return fromVariablesSheet;
    }
}
