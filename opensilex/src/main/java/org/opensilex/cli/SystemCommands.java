//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.opensilex.OpenSilex;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.module.ModuleUpdate;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * <pre>
 * This class regroup all commands concerning OpenSilex system operation
 * install: install module(s)
 * - update: Update module(s)
 * TODO - uninstall: uninstall one or all modules
 * TODO - check: check system configuration
 * </pre>
 */
@Command(
        name = "system",
        header = "Subcommand to group OpenSILEX system operations"
)
public class SystemCommands extends HelpPrinterCommand implements OpenSilexCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(SystemCommands.class);

    private final static String INSTALL_STATE_FILE = "opensilex.install.state.yml";

    private final static String INSTALLED_MODULES_KEY = "installedModules";

    private final static String UPDATES_DATE_MAP_KEY = "updatesDone";

    private Set<String> installedModules;
    private Map<String, LocalDateTime> lastUpdatesByModules;

    private File getInstallationStateFile() {
        return OpenSilex.getInstance().getBaseDirectory().resolve(INSTALL_STATE_FILE).toFile();
    }

    private void readInstallationState() throws IOException {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode node = yamlMapper.readTree(getInstallationStateFile());

        installedModules = new HashSet<>();
        node.at("/" + INSTALLED_MODULES_KEY).elements().forEachRemaining((JsonNode installedModuleNode) -> {
            installedModules.add(installedModuleNode.asText());
        });

        lastUpdatesByModules = new HashMap<>();
        node.at("/" + UPDATES_DATE_MAP_KEY).fields().forEachRemaining((Entry<String, JsonNode> keyValue) -> {
            lastUpdatesByModules.put(keyValue.getKey(), LocalDateTime.parse(keyValue.getValue().asText(), DateTimeFormatter.ISO_DATE));
        });

    }

    private void writeInstallationState() {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        ObjectNode root = mapper.createObjectNode();

        ArrayNode installedModulesNode = root.putArray(INSTALLED_MODULES_KEY);
        for (String module : installedModules) {
            installedModulesNode.add(module);
        }

        ObjectNode updatesDateMap = root.putObject(UPDATES_DATE_MAP_KEY);
        lastUpdatesByModules.forEach((String module, LocalDateTime lastUpdate) -> {
            updatesDateMap.put(module, lastUpdate.format(DateTimeFormatter.ISO_DATE));
        });

        try (FileOutputStream fos = new FileOutputStream(getInstallationStateFile())) {
            yamlFactory.createGenerator(fos).writeObject(root);
        } catch (IOException ex) {
            LOGGER.error("Error while writting installation state of OpenSilex", ex);
            LOGGER.error("Please update manually whith followin content the file:" + getInstallationStateFile().getAbsolutePath());
            LOGGER.error(INSTALLED_MODULES_KEY + ":");
            installedModules.forEach((String module) -> {
                LOGGER.error("  - " + module);
            });
            LOGGER.error(UPDATES_DATE_MAP_KEY + ":");
            lastUpdatesByModules.forEach((String module, LocalDateTime lastUpdate) -> {
                LOGGER.error("  " + module + ": " + lastUpdate.format(DateTimeFormatter.ISO_DATE));
            });
        }
    }

    private boolean isInstalled() {
        return getInstallationStateFile().exists();
    }

    /**
     * This method install OpenSilex specific module or all new if no parameters
     * @param moduleNames List of module names to install
     */
    @Command(
            name = "install",
            header = "Install OpenSilex new module(s)"
    )
    public void install(
            @Parameters(description = "List of module names to install") Set<String> moduleNames
    ) {
        if (isInstalled() && moduleNames.size() == 0) {
            LOGGER.warn("Nothing to do, OpenSilex is already installed, do you mean `opensilex system update` instead ?");
        } else {
            if (!isInstalled()) {
                if (moduleNames.size() != 0) {
                    LOGGER.warn("OpenSilex is not yet installed, all modules will be installed (parameters are ignored)");
                    moduleNames.clear();
                }

                installedModules = new HashSet<>();
                lastUpdatesByModules = new HashMap<>();
            } else {
                try {
                    readInstallationState();
                } catch (IOException ex) {
                    LOGGER.error("Error while reading installation state, abort installation", ex);
                    return;
                }
            }

            Map<String, LocalDateTime> existingUpdatesByModules = new HashMap<>();
            ServiceLoader.load(ModuleUpdate.class, Thread.currentThread().getContextClassLoader()).forEach((ModuleUpdate update) -> {
                String updateProjectId = ClassInfo.getProjectIdFromClass(update.getClass());
                if (!updateProjectId.isEmpty()) {
                    if (existingUpdatesByModules.containsKey(updateProjectId)) {
                        if (existingUpdatesByModules.get(updateProjectId).isBefore(update.getDate())) {
                            existingUpdatesByModules.put(updateProjectId, update.getDate());
                        }
                    } else {
                        existingUpdatesByModules.put(updateProjectId, update.getDate());
                    }
                }
            });

            for (OpenSilexModule module : OpenSilex.getInstance().getModules()) {
                Class<?> moduleClass = module.getClass();
                String moduleProjectId = ClassInfo.getProjectIdFromClass(moduleClass);
                if (!moduleProjectId.isEmpty()) {
                    try {
                        if (moduleNames.size() == 0 || moduleNames.contains(moduleProjectId)) {
                            LOGGER.info("Installing module: " + moduleProjectId + " - " + moduleClass.getCanonicalName());
                            module.install();
                            installedModules.add(moduleProjectId);

                            LOGGER.info("Register all existing updates for installed module:" + moduleProjectId);
                            if (existingUpdatesByModules.containsKey(moduleProjectId)) {
                                lastUpdatesByModules.put(moduleProjectId, existingUpdatesByModules.get(moduleProjectId));
                            }
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Error while installing OpenSilex module: " + moduleProjectId);
                        LOGGER.error("Module class: " + moduleClass.getCanonicalName(), ex);
                    }
                }
            }

            writeInstallationState();
        }
    }

    /**
     * This method updates specific OpenSilex module or all new by default by
     * executing ModuleUpdate "execute" methods found in them
     * @param moduleNames List of module names to update
     */
    @Command(
            name = "update",
            header = "Execute opensilex module(s) update(s)"
    )
    public void update(
            @Parameters(description = "List of module names to update") Set<String> moduleNames
    ) {

        if (!isInstalled()) {
            LOGGER.error("OpenSilex is not installed yet, please execute `opensilex system install` first");
        } else {
            try {
                readInstallationState();
            } catch (Exception ex) {
                LOGGER.error("Can't get existing updates, abort...", ex);
                return;
            }

            // Map to store all new updates by packages
            List<ModuleUpdate> newUpdates = new ArrayList<>();

            // Load all ModuleUpdate and filter the new ones
            ServiceLoader.load(ModuleUpdate.class, Thread.currentThread().getContextClassLoader())
                    .forEach((ModuleUpdate update) -> {
                        // Get the related module id of the update
                        String moduleId = ClassInfo.getProjectIdFromClass(update.getClass());

                        if (!moduleId.isEmpty()) {
                            // Flag to determine if the update must be ignored or not
                            boolean ignore = false;

                            // Determine if the update is a new one for the package
                            if (lastUpdatesByModules.containsKey(moduleId)) {
                                LocalDateTime lastUpdate = lastUpdatesByModules.get(moduleId);
                                ignore = update.getDate().isAfter(lastUpdate);
                            }

                            if (!ignore) {
                                // In that case add it to the update list by date
                                newUpdates.add(update);
                            }
                        }
                    });

            // Sort new update from oldest to newest date
            newUpdates.sort((ModuleUpdate u1, ModuleUpdate u2) -> {
                return u1.getDate().compareTo(u2.getDate());
            });

            // Execute all updates in order
            for (ModuleUpdate update : newUpdates) {
                try {
                    update.execute();
                    lastUpdatesByModules.put(update.getClass().getPackage().getName(), update.getDate());
                } catch (Exception ex) {
                    LOGGER.error("Error while executing update for: " + update.getClass().getCanonicalName() + " " + update.getDate().format(DateTimeFormatter.ISO_DATE), ex);
                    break;
                }
            }

            writeInstallationState();

        }
    }
    
    @Command(
            name = "run-update",
            header = "Execute opensilex module specific update"
    )
    public void runUpdate(
            @Parameters(description = "Update class to execute") String updateClassName
    ) throws Exception {
        Class updateClass = Class.forName(updateClassName, true, Thread.currentThread().getContextClassLoader());
        ModuleUpdate updateInstance = (ModuleUpdate) updateClass.getConstructor().newInstance();
        updateInstance.execute();
    }
}
