//******************************************************************************
//                            SystemCommand.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 09 August 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.commons.io.FileUtils;
import org.opensilex.OpenSilex;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import org.opensilex.module.ModuleUpdate;

/**
 * This class regroup all commands concerning OpenSilex system operation
 * TODO - install: install one or all modules 
 * TODO - uninstall: uninstall one or all modules 
 * - update: Update all modules
 * TODO - check: check system configuration
 */
@Command(
        name = "system",
        header = "Subcommand to group OpenSILEX system operations"
)
public class SystemCommand extends HelpPrinterCommand implements SubCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(SystemCommand.class);

    /**
     * Local file to store all previous updates done
     */
    private final static String UPDATE_FILE = ".opensilex.updates";

    /**
     * This method updates all OpenSilex module by executing all new
     * org.opensilex.module.ModuleUpdate "execute" methods
     *
     * @throws IOException
     */
    @Command(
            name = "update",
            header = "Execute opensilex modules updates"
    )
    public void update() throws IOException {

        // Map to store the last known update date of each package
        Map<String, LocalDate> lastUpdateByPackage = new HashMap<>();

        // Get file where last package updates are stored
        File updateFile = OpenSilex.getInstance().getBaseDirectory().resolve(UPDATE_FILE).toFile();

        if (updateFile.isFile()) {
            // If file exists for each line <package-id>=<last-update-date> add it to the map
            for (String packageLastUpdate : FileUtils.readLines(updateFile, StandardCharsets.UTF_8.name())) {
                String[] packageUpdateArray = packageLastUpdate.split("=", 2);
                if (packageUpdateArray.length != 2) {
                    LOGGER.warn("Invalid package last update property: " + packageLastUpdate);
                } else {
                    lastUpdateByPackage.put(packageUpdateArray[0], LocalDate.parse(packageUpdateArray[1], DateTimeFormatter.ISO_DATE));
                }
            }

            // Map to store all available updates by packages
            List<ModuleUpdate> validUpdates = new ArrayList<>();

            // Load all org.opensilex.module.ModuleUpdate and filter the new ones
            ServiceLoader.load(ModuleUpdate.class, Thread.currentThread().getContextClassLoader())
                    .forEach((ModuleUpdate update) -> {
                        // Get the package name where the class belong
                        String packageName = update.getClass().getPackage().getName();

                        // Flag to determine if the update must be ignored or not
                        boolean ignore = false;

                        // Determine if the update is a new one for the package
                        if (lastUpdateByPackage.containsKey(packageName)) {
                            LocalDate lastUpdate = lastUpdateByPackage.get(packageName);
                            ignore = update.getDate().isAfter(lastUpdate);
                        }

                        if (!ignore) {
                            // In that case add it to the update list by date
                            validUpdates.add(update);
                        }
                    });

            // Sort update from oldest to newest date
            validUpdates.sort((ModuleUpdate u1, ModuleUpdate u2) -> {
                return u1.getDate().compareTo(u2.getDate());
            });

            // Execute all updates in order
            for (ModuleUpdate update : validUpdates) {
                try {
                    update.execute();
                    lastUpdateByPackage.put(update.getClass().getPackage().getName(), update.getDate());
                } catch (Exception ex) {
                    LOGGER.error("Error while executing update for: " + update.getClass().getCanonicalName() + " " + update.getDate().format(DateTimeFormatter.ISO_DATE), ex);
                    break;
                }
            }

            // Update file with updates last date by package name
            FileUtils.deleteQuietly(updateFile);
            List<String> updateFileContent = new ArrayList<>();
            lastUpdateByPackage.forEach((String packageName, LocalDate updateDate) -> {
                updateFileContent.add(packageName + "=" + updateDate.format(DateTimeFormatter.ISO_DATE));
            });

            FileUtils.writeLines(updateFile, updateFileContent, StandardCharsets.UTF_8.name());
        } else {
            // If file doesn't exists inform user that they should execute system install first
            LOGGER.error("No OpenSilex installation found at: " + OpenSilex.getInstance().getBaseDirectory().toString());
            LOGGER.error("Please execute `opensilex system install` first");
        }
    }
}
