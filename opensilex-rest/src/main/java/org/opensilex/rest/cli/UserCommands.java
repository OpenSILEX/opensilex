//******************************************************************************
//                          UserCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.cli;

import javax.mail.internet.InternetAddress;
import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.help.HelpOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
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
     * @throws Exception if command fail
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
            @CommandLine.Option(names = {"--lang"}, description = "Define if user default language", defaultValue = OpenSilex.DEFAULT_LANGUAGE) String lang,
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        
        OpenSilex opensilex = OpenSilex.getInstance();
        
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

        UserDAO userDAO = new UserDAO(sparql);

        String passwordHash = authentication.getPasswordHash(password);
        UserModel user = userDAO.create(null, new InternetAddress(email), firstName, lastName, isAdmin, passwordHash, lang);
        
        LOGGER.info("User created: " + user.getUri());
        factory.dispose(sparql);
    }
    
    public static void main(String[] args) {
        MainCommand.main(new String[]{
            "user",
            "add",
            "--admin",
            "--CONFIG_FILE=/home/vmigot/sources/opensilex-dev/opensilex-dev-tools/src/main/resources/config/opensilex.yml"
        });
    }
    
}
