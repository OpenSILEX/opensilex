/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

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
 * Encapsulates search results.
 * <p/>
 * Items' subclass can be identified from the 'type' value.
 * 
 * @author rspace
 *
 */
@Data
public class SearchResults <T extends Item> {

	String q;
	@JsonProperty(value = "total_count")
	private int totalCount;
	@JsonProperty(value = "count_in_response")
	private int countInResponse;
	int start;

	List<Object> spellingAlternatives;
	List<T> items;
	
	/**
	 * Filters a list of SearchHits by their type.
	 * @param type
	 * @return
	 */
	public List<Item> filterByType (SearchType type) {
		return items.stream().filter((i)->
		 i.getType().equalsIgnoreCase(type.name()))
		.collect(Collectors.toList());
	}

}
