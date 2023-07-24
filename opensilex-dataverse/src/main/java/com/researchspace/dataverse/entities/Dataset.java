/*
 * 
 */
package com.researchspace.dataverse.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URL;
import java.util.Optional;
/** <pre>
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
@Data
@AllArgsConstructor()
@NoArgsConstructor()
public class Dataset {
	private DatasetVersion datasetVersion, latestVersion;
	private Long id;
	private String identifier, protocol, authority;
	private URL persistentUrl;
	
	/**
	 * Getter for the DOI String used to identify a dataset for SWORD upload
	 * @return an {@link Optional}. Will be <code>null</code> if <code>persistentURL</code> is not set.
	 */
	public Optional<String> getDoiId (){
		if(persistentUrl == null) {
			return Optional.empty();
		} else {
			return Optional.of(getPersistentUrl().getPath().substring(1));
		}
	}
}
