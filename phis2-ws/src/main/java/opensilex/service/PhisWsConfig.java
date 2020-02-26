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
            value = "Infrastructure name used for URI generation"
    )
    String infrastructure();

    @ConfigDescription(
            value = "RDF Vocabulary context",
            defaultString = "http://www.opensilex.org/vocabulary/oeso"
    )
    String vocabulary();

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
