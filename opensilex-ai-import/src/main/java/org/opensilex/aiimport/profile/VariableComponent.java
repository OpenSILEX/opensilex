//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

/**
 * One of the parts a variable is made of, as the file names it.
 * <p>
 * An OpenSILEX variable is an entity, a characteristic, a method and a unit, and none of those can
 * be invented at insertion time. Most templates give none of them; a checklist like MIAPPE gives
 * the trait, the method and the scale, each optionally with an ontology accession. Keeping them
 * here is what lets a variable be proposed for creation instead of merely reported as missing.
 *
 * @author Arnaud Charleroy
 */
public class VariableComponent {

    private final String name;

    /**
     * An ontology accession when the file gives one, e.g. {@code CO_322:0000030}. The most
     * reliable matching key there is, and the only one that survives a translation.
     */
    private final String accession;

    public VariableComponent(String name, String accession) {
        this.name = name == null ? null : name.trim();
        this.accession = accession == null || accession.trim().isEmpty() ? null : accession.trim();
    }

    public String getName() {
        return name;
    }

    public String getAccession() {
        return accession;
    }

    public boolean isEmpty() {
        return (name == null || name.isEmpty()) && accession == null;
    }

    @Override
    public String toString() {
        return accession == null ? String.valueOf(name) : name + " (" + accession + ")";
    }
}
