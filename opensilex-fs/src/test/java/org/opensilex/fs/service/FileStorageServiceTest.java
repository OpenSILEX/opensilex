package org.opensilex.fs.service;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.OpenSilex;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileStorageServiceTest {

    private static OpenSilex openSilex;
    protected static FileStorageService fs;

    @BeforeClass
    public static void beforeClass() throws Exception {

        // define an OpenSILEX config with use grids as filesystem
        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);

        // initialize OpenSILEX with this config
        openSilex = OpenSilex.createInstance(args);
        fs = openSilex.getServiceInstance(FileStorageService.DEFAULT_FS_SERVICE, FileStorageService.class);
    }

    @Test
    public void getFilePathFromPrefixURI() {

        String expectedPrefix = "prefix/id/document/rdf_database_systems/";
        URI absoluteURI = URI.create("http://www.opensilex.com/id/document/rdf_database_systems");
        URI shortURI = URI.create("test:id/document/rdf_database_systems");

        Path pathFromAbsoluteURI = fs.getFilePathFromPrefixURI("prefix", absoluteURI);
        Assert.assertNotNull(pathFromAbsoluteURI);
        Assert.assertTrue(pathFromAbsoluteURI.toString().startsWith(expectedPrefix));

        Path pathFromShortURI = fs.getFilePathFromPrefixURI("prefix", absoluteURI);
        Assert.assertNotNull(pathFromShortURI);
        Assert.assertTrue(pathFromShortURI.toString().startsWith(expectedPrefix));
    }
}