//******************************************************************************
//                          UserCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.opensilex.OpenSilex;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.OpenSilexModule;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.help.HelpOption;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import org.opensilex.module.OpenSilexModuleUpdate;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import picocli.CommandLine;

/**
 * @author Vincent Migot
 */
@Command(
        name = "user",
        header = "Subcommand to group OpenSILEX users operations"
)
public class UserCommands extends HelpPrinterCommand implements OpenSilexCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserCommands.class);

    /**
    /**
     * This method add a user to OpenSilex instance
     *
     * @param firstName First name of the user
     * @param lastName Last name of the user
     * @param email Email of the user
     * @param password Password of the user
     * @param isAdmin Flag to determine if user is admin or not
     * @param help Helper to generate automatically command help message
     * @throws Exception 
     */
    @Command(
            name = "add",
            header = "Add an OpenSilex user"
    )
    public void add(
            @CommandLine.Option(names = {"--firstName"}, description = "Define user first name", defaultValue = "Admin") String firstName,
            @CommandLine.Option(names = {"--lastName"}, description = "Define user last name", defaultValue = "OpenSilex") String lastName,
            @CommandLine.Option(names = {"--email"}, description = "Define user email", defaultValue = "admin@opensilex.org") String email,
            @CommandLine.Option(names = {"--password"}, description = "Define user password", defaultValue = "admin") String password,
            @CommandLine.Option(names = {"--admin"}, description = "Define if user is admin", defaultValue = "false") boolean isAdmin,
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        
        OpenSilex opensilex = OpenSilex.getInstance();
        
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);
        AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

        UserDAO userDAO = new UserDAO(sparql, authentication);

        UserModel user = userDAO.create(null, new InternetAddress(email), firstName, lastName, isAdmin, password);
        
        LOGGER.info("User created: " + user.getUri());
    }
}
