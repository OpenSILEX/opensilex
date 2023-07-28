/*
 * 
 */
package com.researchspace.dataverse.api.v1;

import com.researchspace.dataverse.entities.*;
import com.researchspace.dataverse.entities.facade.DatasetFacade;

import java.io.IOException;
import java.util.List;
/** 
 * <pre>
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

*/
public interface DataverseOperations {
	
	
	/**
	 * Create a new dataverse. The supplied {@link DataversePost} must contain as a minimum:
	 *  <ul>
	 *   <li> alias
	 *   <li> Name
	 *    <li> At least one contact email
	 *  </ul>
	 *  @param parentDataverseAlias The owning Dataverse
	 *  @throws IllegalArgumentException if any mandatory fields are <code>null</code>
	 */
	public DataverseResponse<DataversePost> createNewDataverse (String parentDataverseAlias, DataversePost toCreate);
	
	/**
	 * Creates a new Dataset within the specified dataverse.
	 * @param facade
	 * @param dataverseAlias
	 * @return The id of the created dataset
	 * @throws IOException
	 */
	Identifier createDataset(DatasetFacade facade, String dataverseAlias);


	Identifier createDataset(String dataSetJson, String dataverseAlias);
	
    /**
     * Gets an overview of the contents of the specified Dataverse
     * @param dataverseAlias
     * @return 
     */
	List<DataverseObject> getDataverseContents(String dataverseAlias);
	
	/**
	 * Returns complete information on the dataverse 
	 * @param dataverseAlias
	 * @return
	 */
	DataverseGet getDataverseById(String dataverseAlias);

	/**
	 * Deletes a dataverse
	 * @param dataverseAlias numeric or unique identifier of the dataverse
	 * @return A DataverseResponse<DvMessage><DvMessage>. If deleted successfully,  getData will contain a message.
	 *  If status is error ( e.g. couldn't be deleted) the DataverseResponse will contain a message
	 */
	 DataverseResponse<DvMessage> deleteDataverse(String dataverseAlias);

	/**
	 * Publishes the specified Datavers
	 * @param dataverseAlias numeric or unique identifier of the dataverse
	 * @return The updated {@link DataversePost} object
	 */
	DataverseResponse<DataversePost> publishDataverse(String dataverseAlias);

}
