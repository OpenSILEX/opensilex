/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * /** <pre>
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
 * Base class of SearchHits returned in the <code>items</code> field of a search
 * result.
 * 
 * @author rspace
 *
 */
// used to deserialise Json into correct subclasses using value of 'type'
// property
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = DataverseItem.class, name = "dataverse"),
		@Type(value = DatasetItem.class, name = "dataset"), 
		@Type(value = FileSearchHit.class, name = "file"), })
@Data
public abstract class Item {
	private String name, type, url;

	@JsonProperty(value = "image_url")
	private String imageUrl;


}
