/*
 * 
 */
package com.researchspace.dataverse.api.v1;

import com.researchspace.dataverse.entities.*;
import com.researchspace.dataverse.entities.facade.DatasetFacade;
import com.researchspace.dataverse.http.FileUploadMetadata;

import java.io.File;
import java.io.InputStream;
import java.util.List;
/**
<pre>
  Copyright 2016 ResearchSpace

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 </pre>
   
 * @author richard
 *
 * Operations on Datasets
 *
 *
 */
public interface DatasetOperations {

	/**
	 * @param facade The {@link DatasetFacade} containing the updated DataSet data.
	 * @param id The {@link DataSetMetadataBlock} identifier
	 * @return A {@link DatasetVersion}
	 */
	DatasetVersion updateDataset(DatasetFacade facade, Identifier id);

	/**
	 * Retrieves a {@link Dataset} based on its Id.
	 * @param dsIdentifier
	 * @return
	 */
	Dataset getDataset(Identifier dsIdentifier);

	/**
	 * Gets list of metadata about different Dataset versions.
	 * @param dsIdentifier
	 * @return
	 */
	List<DatasetVersion> getDatasetVersions(Identifier dsIdentifier);

	/**
	 * Upload a file to a dataset using Dataverse's native API (not Sword)
	 * @param metadata Metadata to attach to the file upload
	 * @param dsIdentifier The persistent identifier of the dataset
	 * @param data bytes of data to upload
	 * @param fileName The name of the file to be created on Dataverse
	 * @return DatasetFileList information about the uploaded file.
	 */
    DatasetFileList uploadNativeFile( byte[] data, FileUploadMetadata metadata, Identifier dsIdentifier, String fileName);

	/**
	 * Upload a file to a dataset using Dataverse's native API (not Sword).
	 * @param metadata Metadata to attach to the file upload
	 * @param contentLength The length of the stream
	 * @param dsIdentifier The persistent identifier of the dataset
	 * @param data bytes of data to upload
	 * @param fileName The name of the file to be created on Dataverse
	 * @return DatasetFileList information about the uploaded file.
	 */
	DatasetFileList uploadNativeFile(InputStream data, long contentLength, FileUploadMetadata metadata,
									 Identifier dsIdentifier,  String fileName);

    /**
	 * Uploads a file to a dataset
	 * @param doi The DOI of the  Dataset
	 * @param file The file to add to the DataSet
	 */
	void uploadFile(String doi, File file);

	/**
	 * Uploads a file using a data stream.
	 *
	 * @param doi Identifier of the dataset that we are sending the data to.
	 * @param inputStream Stream of data to upload as a file in Dataverse.
	 * @param filename Contents of the field "name" that will appear as in Dataverse.
	 */
	void uploadFile(String doi, InputStream inputStream, String filename);

	/**
	 * Deletes a {@link Dataset}  
	 * @param dsIdentifier
	 * @return
	 */
	DvMessage deleteDataset(Identifier dsIdentifier);
	
	/**
	 * Publishes a DataSet, if the parent dataverse is published.
	 * @param dsIdentifier
	 * @param version Major/Minor
	 * @return A {@link DataverseResponse} with  an error message if Dataset could not be published.
	 */
	DataverseResponse<PublishedDataset> publishDataset(Identifier dsIdentifier, Version version);
}
