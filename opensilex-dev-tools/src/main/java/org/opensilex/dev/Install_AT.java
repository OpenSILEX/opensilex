//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import javax.mail.internet.InternetAddress;
import opensilex.service.PhisPostgreSQLConfig;
import opensilex.service.PhisWsConfig;
import opensilex.service.PhisWsModule;
import org.apache.jena.riot.Lang;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.opensilex.OpenSilex;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.project.dal.ProjectDAO;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class Install_AT {

    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilex.class);

    private static OpenSilex opensilex;

    private static boolean deleteFirst = false;

    public static void main(String[] args) throws Exception {
        Install_AT.deleteFirst = deleteFirst;
        String configFile = getResourceFile("./config/opensilex.yml").getCanonicalPath();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        });

        opensilex = OpenSilex.getInstance();
        createExpriments();
    }

    private static File getResourceFile(String path) {
        Path currentDirectory = Paths.get(System.getProperty("user.dir"));
        return currentDirectory.resolve("./src/main/resources/").resolve(path).toFile();
    }

    private static void createExpriments() throws Exception {
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);
        AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql);
        UserDAO userDAO = new UserDAO(sparql, authentication);
        ProjectDAO projectDAO = new ProjectDAO(sparql);
        
        ProjectModel project = new ProjectModel();
        project.setName("Test 1");
        projectDAO.create(project);
        
        UserModel user = userDAO.getByEmail(new InternetAddress("admin@opensilex.org"));
                
        ExperimentModel experiment = new ExperimentModel();
        experiment.setLabel("Experiment 1");
        experiment.setComment("Comment");
        experiment.setCampaign(2020);
        experiment.setEndDate(LocalDate.now());
        experiment.setStartDate(LocalDate.now());
        //experiment.setGroups(groups);
        //experiment.setLabel(label);
        //experiment.setSpecies(species);
        experiment.setProjects(Arrays.asList(project));
        //experiment.setKeywords(keywords);
        experiment.setTechnicalSupervisors(Arrays.asList(user));
        
        
        
        experimentDAO.create(experiment);

        
    }
}
