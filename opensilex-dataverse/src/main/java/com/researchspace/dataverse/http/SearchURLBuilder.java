/*
 * 
 */
package com.researchspace.dataverse.http;

import com.researchspace.dataverse.search.entities.SearchConfig;
import com.researchspace.dataverse.search.entities.SearchType;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.EnumSet;

import static org.apache.commons.lang.StringUtils.isEmpty;
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
class SearchURLBuilder {
	String buildSearchUrl(String path, SearchConfig cfg) {

		UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(path).queryParam("q", cfg.getQ());
		EnumSet<SearchType> types = cfg.getType();
		if (types != null && !types.isEmpty()) {
			urlBuilder.queryParam("type", types.toArray());
		}
		if (!isEmpty(cfg.getSubtree())) {
			urlBuilder.queryParam("subtree", cfg.getSubtree());
		}
		if (!isEmpty(cfg.getFilterQuery())) {
			urlBuilder.queryParam("fq", cfg.getFilterQuery());
		}
		if (cfg.getSortBy() != null) {
			urlBuilder.queryParam("sort", cfg.getSortBy());
		}
		if (cfg.getSortOrder() != null) {
			urlBuilder.queryParam("order", cfg.getSortOrder());
		}
		if (cfg.getPerPage() != 0) {
			urlBuilder.queryParam("per_page", cfg.getPerPage());
		}
		if (cfg.getStart() != 0) {
			urlBuilder.queryParam("start", cfg.getStart());
		}
		if (cfg.isShowFacets()) {
			urlBuilder.queryParam("show_facets", true);
		}
		if (cfg.isShowRelevance()) {
			urlBuilder.queryParam("show_relevance", true);
		}
		path = urlBuilder.build(true).toUriString();
		return path;
	}

}
