/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "system",
        header = "Subcommand to group OpenSILEX system operations"
)
public class SystemCommand extends HelpPrinterCommand implements SubCommand {

    private final static String UPDATE_FILE = ".opensilex.updates";
    private final static Logger LOGGER = LoggerFactory.getLogger(SystemCommand.class);

    @Command(
            name = "update",
            header = "Execute opensilex modules updates"
    )
    public void update(
            @CommandLine.Parameters(description = "Base directory", defaultValue = "") Path baseDirectory
    ) throws IOException {

        Map<String, LocalDate> lastUpdateByPackage = new HashMap<>();
        File updateFile = baseDirectory.resolve(UPDATE_FILE).toFile();

        if (updateFile.isFile()) {
            for (String packageLastUpdate : FileUtils.readLines(updateFile, StandardCharsets.UTF_8.name())) {
                String[] packageUpdateArray = packageLastUpdate.split("=", 2);
                if (packageUpdateArray.length != 2) {
                    LOGGER.warn("Invalid package last update property: " + packageLastUpdate);
                } else {
                    lastUpdateByPackage.put(packageUpdateArray[0], LocalDate.parse(packageUpdateArray[1], DateTimeFormatter.ISO_DATE));
                }
            }
        }

        Map<String, SortedMap<LocalDate, Update>> updatesByPackage = new HashMap<>();

        ServiceLoader.load(Update.class, Thread.currentThread().getContextClassLoader())
                .forEach((Update update) -> {
                    String packageName = update.getClass().getPackage().getName();

                    boolean ignore = false;
                    if (lastUpdateByPackage.containsKey(packageName)) {
                        LocalDate lastUpdate = lastUpdateByPackage.get(packageName);
                        ignore = update.getDate().isAfter(lastUpdate);
                    }

                    if (!ignore) {
                        SortedMap<LocalDate, Update> updatesByDate = updatesByPackage.get(packageName);
                        if (updatesByDate == null) {
                            updatesByDate = new TreeMap<LocalDate, Update>();
                            updatesByPackage.put(packageName, updatesByDate);
                        }

                        updatesByDate.put(update.getDate(), update);
                    }
                });

        boolean exitLoop = false;
        for (String packageName : updatesByPackage.keySet()) {
            SortedMap<LocalDate, Update> orderedUpdates = updatesByPackage.get(packageName);

            for (LocalDate updateDate : orderedUpdates.keySet()) {
                Update update = orderedUpdates.get(updateDate);
                try {
                    update.execute();
                } catch (Exception ex) {
                    LOGGER.error("Error while executing update for: " + packageName + " " + updateDate.format(DateTimeFormatter.ISO_DATE), ex);
                    exitLoop = true;
                    break;
                }
                
                lastUpdateByPackage.put(packageName, updateDate);
            }
            
            if (exitLoop) {
                break;
            }
        }
        
        FileUtils.deleteQuietly(updateFile);
        List<String> updateFileContent = new ArrayList<>();
        lastUpdateByPackage.forEach((String packageName, LocalDate updateDate) -> {
            updateFileContent.add(packageName + "=" + updateDate.format(DateTimeFormatter.ISO_DATE));
        });
        
        FileUtils.writeLines(updateFile, updateFileContent, StandardCharsets.UTF_8.name());
    }
}
