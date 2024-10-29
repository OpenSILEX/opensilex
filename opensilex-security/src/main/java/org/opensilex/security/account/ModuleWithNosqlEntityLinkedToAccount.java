package org.opensilex.security.account;

import java.net.URI;

/**
 * Interface used for deletion protection of accounts. It allows to check if an account is linked to a nosql entity without adding dependencies from Security Module to other modules.
 */
public interface ModuleWithNosqlEntityLinkedToAccount {
    boolean accountIsLinkedWithANosqlEntity(URI accountURI);
}
