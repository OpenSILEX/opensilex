
/*******************************************************************************
 *                         HttpFileSystemConnection.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Last Modification: 17/11/2021 11:33
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.fs.service;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * @author rcolin
 */
public class HttpFileSystemConnection extends BaseService implements FileStorageConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpFileSystemConnection.class);
    private static final String CONTENT_TYPE = "Content-Type";

    public HttpFileSystemConnection() {
        super(null);
    }

    public HttpFileSystemConnection(ServiceConfig config){
        super(config);
    }

    protected Map<String,String> getRequestProperties(){
        return Collections.emptyMap();
    }

    protected void checkURL(URL url){
        if(StringUtils.isEmpty(url.getHost())){
            throw new IllegalArgumentException("No host from parsed URL "+url);
        }

    }

    protected HttpURLConnection buildHttpGETConnection(Path filePath) throws IOException {

        URL url = new URL(filePath.toString());
        checkURL(url);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        connection.setRequestMethod(HttpMethod.GET);
        connection.setRequestProperty(CONTENT_TYPE,MediaType.APPLICATION_OCTET_STREAM);

        Map<String,String> requestProperties = getRequestProperties();
        if(!MapUtils.isEmpty(requestProperties)){
            requestProperties.forEach(connection::setRequestProperty);
        }

        connection.connect();

        return connection;
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        HttpURLConnection connection = buildHttpGETConnection(filePath);

        /*if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
            String msg = String.format("Bad HTTP status code : %s, expected : %s ",connection.getResponseCode(),HttpURLConnection.HTTP_OK);
            throw new IOException(msg);
        }*/
        byte[] data = IOUtils.toByteArray(connection.getInputStream());
        connection.disconnect();
        return data;
    }

    @Override
    public void writeFile(Path filePath, String content) throws IOException {

    }

    @Override
    public void writeFile(Path filePath, File file) throws IOException {

    }

    @Override
    public void createDirectories(Path directoryPath) throws IOException {

    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        HttpURLConnection connection = buildHttpGETConnection(filePath);
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    @Override
    public void delete(Path filePath) throws IOException {

    }

    @Override
    public Path getAbsolutePath(Path filePath) throws IOException {
        return null;
    }
}
