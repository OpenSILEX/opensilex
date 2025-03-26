package org.opensilex.core.data.factory;

import org.opensilex.core.device.dal.DeviceDAO;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.scientificObject.dal.ScientificObjectDAO;
import org.opensilex.core.variable.dal.VariableDAO;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.service.SPARQLService;

/**
 * A factory class for creating instances of various DAO (Data Access Object) classes.
 * <p>
 * This class centralizes the creation of DAOs, ensuring proper initialization with shared services like
 * SPARQL, NoSQL, file storage, and user authentication. Each DAO is initialized with the dependencies
 * required for its operations.
 * </p>
 *
 * <p>
 * DAOs created by this factory:
 * <ul>
 *     <li>{@link DeviceDAO}</li>
 *     <li>{@link VariableDAO}</li>
 *     <li>{@link ExperimentDAO}</li>
 *     <li>{@link OntologyDAO}</li>
 *     <li>{@link ScientificObjectDAO}</li>
 * </ul>
 * </p>
 *
 * @author Marouan KOURDI
 */

public class DAOFactory {
    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel user;


    /**
     * Constructs a new {@code DAOFactory} with the given services and user account.
     *
     * @param sparql The SPARQL service for interacting with a triple store.
     * @param nosql  The NoSQL service for MongoDB interactions.
     * @param fs     The file storage service for handling file storage operations.
     * @param user   The user account model for managing user-related operations.
     */
    public DAOFactory(SPARQLService sparql, MongoDBService nosql, FileStorageService fs, AccountModel user) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.user = user;
    }

    /**
     * Creates a new instance of {@link DeviceDAO}.
     *
     * @return A {@code DeviceDAO} instance.
     */
    public DeviceDAO createDeviceDAO() {
        return new DeviceDAO(sparql, nosql, fs);
    }

    /**
     * Creates a new instance of {@link VariableDAO}.
     *
     * @return A {@code VariableDAO} instance.
     */
    public VariableDAO createVariableDAO() {
        return new VariableDAO(sparql, nosql, fs, user);
    }

    /**
     * Creates a new instance of {@link ExperimentDAO}.
     *
     * @return A {@code ExperimentDAO} instance.
     */
    public ExperimentDAO createExperimentDAO() {
        return new ExperimentDAO(sparql, nosql);
    }

    /**
     * Creates a new instance of {@link OntologyDAO}.
     *
     * @return A {@code OntologyDAO} instance.
     */
    public OntologyDAO createOntologyDAO() {
        return new OntologyDAO(sparql);
    }

    /**
     * Creates a new instance of {@link ScientificObjectDAO}.
     *
     * @return A {@code ScientificObjectDAO} instance.
     */
    public ScientificObjectDAO createScientificObjectDAO() {
        return new ScientificObjectDAO(sparql, nosql);
    }
}