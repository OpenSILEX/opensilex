// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.sword;

import org.swordapp.client.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Uploads using SWORD client library
 *  Copyright 2016 ResearchSpace
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * This is an internal package for calling SWORD-API.
 * @author rspace
 */
public class FileUploader {
	@SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileUploader.class);
	private static final String APPLICATION_ZIP = "application/zip";
	private static final String ZIP_PACKAGING = "http://purl.org/net/sword/package/SimpleZip";

	/**
	 * @param file
	 * @param apiKey
	 * @param dataverseServer Server root e.g. "https://apitest.dataverse.org"
	 * @param doi FRagment e.g.  "10.5072/FK2/MGADL1"
	 * @return
	 * @throws IOException
	 * @throws SWORDClientException
	 * @throws SWORDError
	 * @throws ProtocolViolationException
	 */
	public DepositReceipt deposit(File file, String apiKey, URI dataverseServer, String doi) throws IOException, SWORDClientException, SWORDError, ProtocolViolationException {
		return this.deposit(new FileInputStream(file), file.getName(), apiKey, dataverseServer, doi);
	}

	/**
	 * Creates a deposit object to upload a file into a dataverse instance using the SWORD library client.
	 *
	 * @param is Data coming as a stream.
	 * @param filename Name of the file to upload.
	 * @param apiKey Key used to authenticate actions into the goal dataverse instance.
	 * @param dataverseServer URL of the dataverse instance to attack.
	 * @param doi To identify the dataset that is the goal of the file upload.
	 * @return Information of the result of the upload via a {@code DepositReceipt} instance.
	 * @throws IOException Thrown when a IO error occurs, which is a general error.
	 * @throws SWORDClientException Thrown when an exception happens inside the SWORD client.
	 * @throws SWORDError Thrown when an exception happens inside the SWORD client.
	 * @throws ProtocolViolationException Thrown for unknown reasons.
	 */
	public DepositReceipt deposit(InputStream is, String filename, String apiKey, URI dataverseServer, String doi) throws IOException, SWORDClientException, SWORDError, ProtocolViolationException {
		SWORDClient cli = new SWORDClient();
		Deposit dep = new Deposit();
		dep.setFilename(filename);
		dep.setFile(is);
		dep.setMimeType(APPLICATION_ZIP);
		dep.setPackaging(ZIP_PACKAGING);
		AuthCredentials cred = new AuthCredentials(apiKey, "");
		String depositURI = dataverseServer.toString() + "/dvn/api/data-deposit/v1.1/swordv2/edit-media/study/doi:" + doi;
		DepositReceipt rct = cli.deposit(depositURI, dep, cred);
		log.info("Deposit received with status {}", rct.getStatusCode());
		return rct;
	}
}
