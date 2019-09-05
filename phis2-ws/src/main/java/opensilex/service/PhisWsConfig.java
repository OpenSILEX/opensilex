/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service;

import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;

/**
 *
 * @author vincent
 */
public interface PhisWsConfig extends ModuleConfig {

    PhisPostgreSQLConfig postgreSQL();

    @ConfigDescription(
            value = "Base URI for plateform URI generation"
    )
    String ontologyBaseURI();

    @ConfigDescription(
            value = "Infrastructure name used for URI generation"
    )
    String infrastructure();

    @ConfigDescription(
            value = "RDF Vocabulary context",
            defaultString = "http://www.opensilex.org/vocabulary/oeso"
    )
    String vocabulary();

    @ConfigDescription(
            value = "Server host for sftp file upload",
            defaultString = "localhost"
    )
    String uploadFileServerIP();

    @ConfigDescription(
            value = "Username for sftp file upload"
    )
    String uploadFileServerUsername();

    @ConfigDescription(
            value = "Password for sftp file upload"
    )
    String uploadFileServerPassword();

    @ConfigDescription(
            value = "Direectory for sftp file upload"
    )
    String uploadFileServerDirectory();

    @ConfigDescription(
            value = "Directory to store layers"
    )
    String layerFileServerDirectory();

    @ConfigDescription(
            value = "Public base URL for layers web access"
    )
    String layerFileServerAddress();

    @ConfigDescription(
            value = "Directory to store images"
    )
    String uploadImageServerDirectory();

    @ConfigDescription(
            value = "Public base URL for images web access"
    )
    String imageFileServerDirectory();

    @ConfigDescription(
            value = "GnpIS public key filename",
            defaultString = "GnpIS-JWT-public-key"
    )
    String gnpisPublicKeyFileName();

    @ConfigDescription(
            value = "PHIS public key filename",
            defaultString = "Phis-JWT-public-key.der"
    )
    String phisPublicKeyFileName();

    @ConfigDescription(
            value = "MongoDB documents storage collection",
            defaultString = "documents"
    )
    String documentsCollection();

    @ConfigDescription(
            value = "MongoDB provenance storage collection",
            defaultString = "provenance"
    )
    String provenanceCollection();

    @ConfigDescription(
            value = "MongoDB data storage collection",
            defaultString = "rawData"
    )
    String dataCollection();

    @ConfigDescription(
            value = "MongoDB image storage collection",
            defaultString = "images"
    )
    String imagesCollection();

    @ConfigDescription(
            value = "Session time",
            defaultString = "12000"
    )
    String sessionTime();

    @ConfigDescription(
            value = "Time to wait for document upload",
            defaultString = "50000"
    )
    String waitingFileTime();

    @ConfigDescription(
            value = "Maximum value for page size",
            defaultString = "2097152"
    )
    String pageSizeMax();

}
