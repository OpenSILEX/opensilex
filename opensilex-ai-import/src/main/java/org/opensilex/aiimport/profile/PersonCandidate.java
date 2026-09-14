//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

/**
 * Someone the file names, to be matched against the people this instance knows.
 * <p>
 * Distinct from an observer name. An observer is a code in a data cell that ends up on a
 * provenance; a person is a resource of its own — the contact of a study, its author, whoever
 * submitted the data — with an email, an ORCID and an affiliation. MIAPPE has a section for exactly
 * that, and reading it as observer codes lost everything but the name.
 *
 * @author Arnaud Charleroy
 */
public class PersonCandidate {

    /**
     * The name as the file writes it, usually whole: "Ines Chaves".
     */
    private String name;

    private String email;

    /**
     * An ORCID when the file gives one. The only identifier here that is globally unique, so it is
     * the best matching key when present.
     */
    private String orcid;

    private String affiliation;

    /**
     * What they did — data submitter, author, corresponding author. OpenSILEX has no field for it,
     * so it is carried for the assistant to mention rather than dropped.
     */
    private String role;

    public PersonCandidate(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getName() {
        return name;
    }

    public PersonCandidate setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public PersonCandidate setEmail(String email) {
        this.email = blankToNull(email);
        return this;
    }

    public String getOrcid() {
        return orcid;
    }

    public PersonCandidate setOrcid(String orcid) {
        this.orcid = blankToNull(orcid);
        return this;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public PersonCandidate setAffiliation(String affiliation) {
        this.affiliation = blankToNull(affiliation);
        return this;
    }

    public String getRole() {
        return role;
    }

    public PersonCandidate setRole(String role) {
        this.role = blankToNull(role);
        return this;
    }

    /**
     * What to match on, best key first: an ORCID identifies one human worldwide, an email one
     * account, a name neither.
     */
    public String getSearchKey() {
        if (orcid != null) {
            return orcid;
        }
        return email != null ? email : name;
    }

    /**
     * The given name, guessed as everything before the last space.
     * <p>
     * A guess, and it is treated as one: the split feeds the person form, where it is visible and
     * correctable, and never a silent write. "Ines Chaves" splits cleanly; "Jean-Pierre de la Rue"
     * does not, and nobody but the user can settle it.
     */
    public String getFirstName() {
        if (name == null) {
            return null;
        }
        int lastSpace = name.trim().lastIndexOf(' ');
        return lastSpace < 0 ? name.trim() : name.trim().substring(0, lastSpace).trim();
    }

    /**
     * The family name, guessed as the last word. See {@link #getFirstName()}.
     */
    public String getLastName() {
        if (name == null) {
            return null;
        }
        int lastSpace = name.trim().lastIndexOf(' ');
        return lastSpace < 0 ? null : name.trim().substring(lastSpace + 1).trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
