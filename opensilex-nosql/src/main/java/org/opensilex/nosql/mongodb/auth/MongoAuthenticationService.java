/*
 *  *************************************************************************************
 *  MongoAuthenticationService.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2023
 * Contact :  renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.nosql.mongodb.auth;

import com.mongodb.MongoCredential;
import org.opensilex.service.Service;

import java.io.IOException;

/**
 * Basic interface which define how to handle authentication when connecting to a MongoDB server
 * @author rcolin
 */
public interface MongoAuthenticationService extends Service {

    /**
     * @return the {@link MongoCredential} which can be read by the {@link com.mongodb.client.MongoClient} in order to perform
     * an authenticated connection
     * @throws SecurityException if the read of credentials can't be done due to security issues
     * @throws IOException If the auth service must deal with I/O file and encounter an error
     *
     */
    MongoCredential readCredentials() throws SecurityException, IOException;

}
