/*
 * 
 */
package com.researchspace.dataverse.api.v1;

import com.researchspace.dataverse.entities.MetadataBlock;

import java.util.List;

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
public interface MetadataOperations {
	
	/**
	 * Return data about the block whose identifier is passed. 
	 *  identifier can either be the block’s id, or its name:
	 *  <p/>
	 * <strong>Possible bug </strong>Doesn't actually work for numeric ids, only name
	 * @param name The MetadataBlock name
	 * @return
	 */
	MetadataBlock getMetadataById(String name);
	
	/**
	 * Lists brief info about all metadata blocks registered in the system:
	 * @return a {@link List} of {@link MetadataBlock}
	 */
	List<MetadataBlock> getMetadataBlockInfo();

}
