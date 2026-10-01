//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * A person the file names: matched on the best key it gives, and split into names only as a guess.
 *
 * @author Arnaud Charleroy
 */
public class PersonCandidateTest {

    @Test
    public void theOrcidIsTheBestKeyThenTheEmailThenTheName() {
        PersonCandidate person = new PersonCandidate("Ines Chaves");
        assertEquals("Ines Chaves", person.getSearchKey());

        person.setEmail("ines.chaves@example.org");
        assertEquals("ines.chaves@example.org", person.getSearchKey());

        person.setOrcid("0000-0002-1825-0097");
        assertEquals("0000-0002-1825-0097", person.getSearchKey());
    }

    @Test
    public void theNameIsSplitOnItsLastSpace() {
        PersonCandidate person = new PersonCandidate("  Jean-Pierre de la Rue ");

        assertEquals("Jean-Pierre de la", person.getFirstName());
        assertEquals("Rue", person.getLastName());
    }

    @Test
    public void aSingleWordIsAGivenNameOnly() {
        PersonCandidate person = new PersonCandidate("Ines");

        assertEquals("Ines", person.getFirstName());
        assertNull(person.getLastName());
        assertNull(new PersonCandidate(null).getFirstName());
        assertNull(new PersonCandidate(null).getLastName());
    }

    @Test
    public void itCarriesWhatTheFileSaysAboutThePerson() {
        PersonCandidate person = new PersonCandidate("Ines Chaves")
                .setAffiliation("INRAE")
                .setRole("contact");
        person.setName("Inès Chaves");

        assertEquals("INRAE", person.getAffiliation());
        assertEquals("contact", person.getRole());
        assertEquals("Inès Chaves", person.getName());
    }
}
