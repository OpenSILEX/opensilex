//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;

/**
 * How the author of a template is named to the users it is shared with: the person behind the
 * account when there is one, else what comes before the {@code @} of the account's address. The
 * address itself is not shown to people who were only given a template.
 *
 * @author Arnaud Charleroy
 */
public final class TemplateAuthor {

    private TemplateAuthor() {
    }

    public static String nameOf(AccountModel account) {
        PersonModel person = account.getLinkedPerson();
        if (person != null && person.getFirstName() != null && person.getLastName() != null) {
            return person.getFirstName() + " " + person.getLastName();
        }
        String address = account.getName();
        int at = address.indexOf('@');
        return at > 0 ? address.substring(0, at) : address;
    }
}
