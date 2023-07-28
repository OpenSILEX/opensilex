/*
 * 
 */
package com.researchspace.dataverse.api.v1;

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
 Top-level entry point into the Dataverse Level1 API
 * @author rspace
 *
 */
public interface DataverseAPI {

	/**
	 * Gets class for performing DatasetOperations
	 * @return
	 */
	DatasetOperations getDatasetOperations ();
	
	/**
	 * Gets class for performing Metadata Operations
	 * @return
	 */
    MetadataOperations getMetadataOperations ();
	
    /**
	 * Gets class for performing Dataverse Operations
	 * @return
	 */
	DataverseOperations getDataverseOperations ();
	
	/**
	 * Configures the connection settings.
	 * @param config
	 */
	void configure (DataverseConfig config);
	
	InfoOperations getInfoOperations();
	
	/**
	 * Accesses the Search API
	 * @return
	 */
	SearchOperations getSearchOperations();


}