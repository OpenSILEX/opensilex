/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 *
 * @author vince
 */
public class OpenSilexSetup {

    private final Path baseDirectory;
    private final String profileId;
    private final File configFile;
    private final boolean debug;
    private final boolean noCache;
    private final String[] args;
    private final List<Object> cliArgsList;

    OpenSilexSetup(Path baseDirectory, String profileId, File configFile, boolean debug, boolean noCache, String[] args, List<Object> cliArgsList) {
        this.baseDirectory = baseDirectory;
        this.profileId = profileId;
        this.configFile = configFile;
        this.debug = debug;
        this.noCache = noCache;
        this.args = args;
        this.cliArgsList = cliArgsList;
    }

    public Path getBaseDirectory() {
        return baseDirectory;
    }

    public String getProfileId() {
        return profileId;
    }

    public File getConfigFile() {
        return configFile;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isNoCache() {
        return noCache;
    }

    public String[] getArgs() {
        return args;
    }

    public List<Object> getCliArgsList() {
        return cliArgsList;
    }
    
    public boolean hasConfigFile() {
        return !(configFile == null || configFile.equals(""));
    }

    public String[] getRemainingArgs() {
        return cliArgsList.toArray(new String[cliArgsList.size()]);
    }

}
