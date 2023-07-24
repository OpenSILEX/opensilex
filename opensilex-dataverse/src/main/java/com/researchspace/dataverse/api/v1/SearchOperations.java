/*
 * 
 */
package com.researchspace.dataverse.api.v1;

import com.researchspace.dataverse.entities.DataverseResponse;
import com.researchspace.dataverse.search.entities.*;
import com.researchspace.dataverse.search.entities.SearchConfig.SearchConfigBuilder;

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
 Top-level entry point into the Dataverse Level1 Search API
 * @author rspace
 *
 */
public interface SearchOperations {
	/**
	 * Gets a new instance of a SearchConfigBuilder to configure a search query
	 * @return
	 */
	SearchConfigBuilder builder();

	/**
	 * Perform a search 
	 * @param cfg A {@link SearchConfig} object generated from a {@link SearchConfigBuilder}
	 * @return
	 */
	DataverseResponse<SearchResults<Item>> search(SearchConfig cfg);

	/**
	 * A search restricted to Dataverse files that returns a typed list of {@link FileSearchHit}.
	 * @param cfg A {@link SearchConfig} configured to search by SearchType.file only
	 * @throws IllegalArgumentException if search config is not set to return files only.
	 */
	DataverseResponse<SearchResults<FileSearchHit>> searchFiles(SearchConfig cfg);

	/**
	 * A search restricted to Dataverses that returns a typed list of {@link DataverseItem}.
	 * @param cfg A {@link SearchConfig} configured to search by SearchType.dataverse only
	 * @throws IllegalArgumentException if search config is not set to return dataverses only.
	 */
	DataverseResponse<SearchResults<DataverseItem>> searchDataverses(SearchConfig cfg);

	/**
	 * A search restricted to Dataverses that returns a typed list of {@link DatasetItem}.
	 * @param cfg A {@link SearchConfig} configured to search by SearchType.dataset only
	 * @throws IllegalArgumentException if search config is not set to return datasets only.
	 */
	DataverseResponse<SearchResults<DatasetItem>> searchDatasets(SearchConfig cfg);

}
