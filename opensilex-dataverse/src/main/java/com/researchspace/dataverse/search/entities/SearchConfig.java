/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

import java.util.EnumSet;

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
 * Read-only search configuration object. <br/>
 * Use the <code>builder()</code> method to return a new SearchConfigBuilder to
 * build a search configuration in a Fluent style.
 * 
 * @author rspace
 *
 */
@Builder(toBuilder = true)
@Value
public class SearchConfig {
	/**
	 * Builder for configuring search via fluent API
	 * 
	 * @author rspace
	 *
	 */
	public static class SearchConfigBuilder {
		/**
		 * Sets results per page. Maximum is 1000
		 * 
		 * @param perPage
		 *            if &gt 1000, will set to 1000
		 * @return
		 * @throws IllegalArgumentException
		 *             if <code>perPage</code> &lt= 0
		 */
		public SearchConfigBuilder perPage(int perPage) {
			Validate.isTrue(perPage > 0, "Cannot have negative results per page");
			if (perPage > MAX_RESULTS_PER_PAGE) {
				perPage = MAX_RESULTS_PER_PAGE;
			}
			this.perPage = perPage;
			return this;
		}

		/**
		 * Sets results per page. Maximum is 1000
		 * 
		 * @param perPage
		 *            if &gt 1000, will set to 1000
		 * @return
		 * @throws IllegalArgumentException
		 *             if <code>perPage</code> &lt= 0
		 */
		public SearchConfigBuilder start(int start) {
			Validate.isTrue(start > 0, "Cannot have negative starting point");
			this.start = start;
			return this;
		}

	}

	public static final int MAX_RESULTS_PER_PAGE = 1000;
	private EnumSet<SearchType> type;
	private @NonNull String q;
	private String subtree, filterQuery;
	private SortBy sortBy;
	private SortOrder sortOrder;
	private int perPage, start;
	private boolean showRelevance, showFacets;

}
