//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.fs.service.FileStorageService;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.preview.NodeComparison;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountDAO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a writer works with: the platform's services, the user it writes for, the experiment of the
 * import, and every resource of the preview by key, so that a link to a row of the file finds that row.
 *
 * @param experiment the experiment chosen for the import; null when none is
 * @param nodes      every resource of the preview, by the key of its node
 * @author Arnaud Charleroy
 */
public record WriteContext(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user,
                           URI experiment, ClassCatalog catalog, Map<String, NodeComparison> nodes) {

    public WriteContext {
        nodes = Map.copyOf(nodes);
    }

    public static WriteContext of(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user,
                                  URI experiment, ClassCatalog catalog, List<NodeComparison> preview) {
        Map<String, NodeComparison> byKey = new HashMap<>();
        preview.forEach(node -> byKey.put(node.node().getKey(), node));
        return new WriteContext(sparql, nosql, fs, user, experiment, catalog, byKey);
    }

    /** Whether the user may create what needs this credential: an administrator always may. */
    public boolean mayWrite(String credentialId) throws Exception {
        if (Boolean.TRUE.equals(user.isAdmin())) {
            return true;
        }
        return new AccountDAO(sparql).getCredentialList(user.getUri()).contains(credentialId);
    }
}
