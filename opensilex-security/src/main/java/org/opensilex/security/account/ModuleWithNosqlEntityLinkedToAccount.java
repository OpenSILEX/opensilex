package org.opensilex.security.account;

import java.net.URI;

public interface ModuleWithNosqlEntityLinkedToAccount {
    boolean accountIsLinkedWithANosqlEntity(URI accountURI);
}
