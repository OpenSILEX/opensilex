/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
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
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class FileSearchHit extends Item {

	private @JsonProperty("file_id") String fileId;
	private @JsonProperty("dataset_citation") String datasetCitation;
	private @JsonProperty("file_content_type") String fileContentType;
	private String description, md5;
	private @JsonProperty("size_in_bytes") int size;
	@JsonProperty("published_at")
	private String publishedAt;

	public String getType() {
		return "file";
	}

}
