//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.graph;

import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;

/**
 * Who looks instances up, and in which experiment. Built only through {@link #of}, which checks that
 * the user may read the experiment: a context never holds an experiment its user cannot see.
 *
 * @author Arnaud Charleroy
 */
public final class LookupContext {

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;
    private final URI experiment;

    private LookupContext(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user,
                          URI experiment) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
        this.experiment = experiment;
    }

    /**
     * @param experiment the experiment scientific objects are looked up in; null when none is chosen
     * @throws org.opensilex.security.authentication.ForbiddenURIAccessException when the user may not read the experiment
     */
    public static LookupContext of(SPARQLService sparql, MongoDBService nosql, FileStorageService fs,
                                   AccountModel user, URI experiment) throws Exception {
        if (experiment != null) {
            new ExperimentDAO(sparql, nosql, fs).validateExperimentAccess(experiment, user);
        }
        return new LookupContext(sparql, nosql, fs, user, experiment);
    }

    public SPARQLService sparql() {
        return sparql;
    }

    public MongoDBService nosql() {
        return nosql;
    }

    public FileStorageService fs() {
        return fs;
    }

    /** The account the lookups are made for. */
    public AccountModel user() {
        return user;
    }

    /** The experiment scientific objects are looked up in; null when none is chosen. */
    public URI experiment() {
        return experiment;
    }
}
