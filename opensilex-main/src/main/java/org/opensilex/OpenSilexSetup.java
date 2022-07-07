//******************************************************************************
//                       OpenSilexModuleNotFoundException.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * OpenSilex setup configuration class.
 *
 * @author Vincent Migot
 */
public class OpenSilexSetup {

    /**
     * System base directory.
     */
    private final Path baseDirectory;

    /**
     * System profile id.
     */
    private final String profileId;

    /**
     * System config file.
     */
    private final File configFile;

    /**
     * System debug flag.
     */
    private final boolean debug;

    /**
     * System global cache disable flag.
     */
    private final boolean noCache;

    /**
     * System command line arguments.
     */
    private final String[] args;

    /**
     * System unused command line arguments to be used by Picocli.
     */
    private final List<Object> cliArgsList;

    /**
     * Constructor simply defining all class final members.
     *
     * @param baseDirectory
     * @param profileId
     * @param configFile
     * @param debug
     * @param noCache
     * @param args
     * @param cliArgsList
     */
    public OpenSilexSetup(Path baseDirectory, String profileId, File configFile, boolean debug, boolean noCache, String[] args, List<Object> cliArgsList) {
        this.baseDirectory = baseDirectory;
        this.profileId = profileId;
        this.configFile = configFile;
        this.debug = debug;
        this.noCache = noCache;
        this.args = args;
        this.cliArgsList = cliArgsList;
    }

    /**
     * Getter for base directory.
     *
     * @return base directory
     */
    public Path getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Return profile identifier.
     *
     * @return profile identifier
     */
    public String getProfileId() {
        return profileId;
    }

    /**
     * Getter for config file.
     *
     * @return config file
     */
    public File getConfigFile() {
        return configFile;
    }

    /**
     * Getter for debug flag.
     *
     * @return debug flag
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Getter for no cache flag.
     *
     * @return no cache flag
     */
    public boolean isNoCache() {
        return noCache;
    }

    /**
     * Getter for command line arguments.
     *
     * @return command line arguments
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * Getter for command line unused arguments.
     *
     * @return unused arguments
     */
    public List<Object> getCliArgsList() {
        return cliArgsList;
    }

    /**
     * Determine if setup has a config file defined.
     *
     * @return true if setup is build with a config file
     */
    public boolean hasConfigFile() {
        return !(configFile == null || configFile.equals(""));
    }

    /**
     * Help method to get remaining command line arguments as a String array.
     *
     * @return command line remaining arguemnts string array.
     */
    public String[] getRemainingArgs() {
        return cliArgsList.toArray(new String[cliArgsList.size()]);
    }

}
