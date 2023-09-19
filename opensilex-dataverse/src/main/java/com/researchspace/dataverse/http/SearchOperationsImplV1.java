// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.http;

import com.researchspace.dataverse.api.v1.SearchOperations;
import com.researchspace.dataverse.entities.DataverseResponse;
import com.researchspace.dataverse.search.entities.*;
import com.researchspace.dataverse.search.entities.SearchConfig.SearchConfigBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * <pre>
 * Copyright 2016 ResearchSpace
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </pre>
 */
public class SearchOperationsImplV1 extends AbstractOpsImplV1 implements SearchOperations {
	@SuppressWarnings("all")
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SearchOperationsImplV1.class);
	private SearchURLBuilder urlBuilder = new SearchURLBuilder();

	@Override
	public SearchConfigBuilder builder() {
		return SearchConfig.builder();
	}

	@Override
	public DataverseResponse<SearchResults<Item>> search(SearchConfig cfg) {
		String url = createV1Url("search");
		url = urlBuilder.buildSearchUrl(url, cfg);
		HttpHeaders headers = addAPIKeyToHeader();
		ParameterizedTypeReference<DataverseResponse<SearchResults<Item>>> type = new ParameterizedTypeReference<DataverseResponse<SearchResults<Item>>>() {
		};
		ResponseEntity<DataverseResponse<SearchResults<Item>>> resp = template.exchange(url, HttpMethod.GET, createHttpEntity(headers), type);
		log.debug(resp.getBody().getData().toString());
		return resp.getBody();
	}

	private HttpEntity<String> createHttpEntity(HttpHeaders headers) {
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		return entity;
	}

	@Override
	public DataverseResponse<SearchResults<FileSearchHit>> searchFiles(SearchConfig cfg) {
		validateSrchConfig(cfg, SearchType.file);
		String url = createV1Url("search");
		url = urlBuilder.buildSearchUrl(url, cfg);
		HttpHeaders headers = addAPIKeyToHeader();
		ParameterizedTypeReference<DataverseResponse<SearchResults<FileSearchHit>>> type = new ParameterizedTypeReference<DataverseResponse<SearchResults<FileSearchHit>>>() {
		};
		ResponseEntity<DataverseResponse<SearchResults<FileSearchHit>>> resp = template.exchange(url, HttpMethod.GET, createHttpEntity(headers), type);
		log.debug(resp.getBody().getData().toString());
		return resp.getBody();
	}

	@Override
	public DataverseResponse<SearchResults<DatasetItem>> searchDatasets(SearchConfig cfg) {
		validateSrchConfig(cfg, SearchType.dataset);
		String url = createV1Url("search");
		url = urlBuilder.buildSearchUrl(url, cfg);
		HttpHeaders headers = addAPIKeyToHeader();
		ParameterizedTypeReference<DataverseResponse<SearchResults<DatasetItem>>> type = new ParameterizedTypeReference<DataverseResponse<SearchResults<DatasetItem>>>() {
		};
		ResponseEntity<DataverseResponse<SearchResults<DatasetItem>>> resp = template.exchange(url, HttpMethod.GET, createHttpEntity(headers), type);
		log.debug(resp.getBody().getData().toString());
		return resp.getBody();
	}

	@Override
	public DataverseResponse<SearchResults<DataverseItem>> searchDataverses(SearchConfig cfg) {
		validateSrchConfig(cfg, SearchType.dataverse);
		String url = createV1Url("search");
		url = urlBuilder.buildSearchUrl(url, cfg);
		HttpHeaders headers = addAPIKeyToHeader();
		ParameterizedTypeReference<DataverseResponse<SearchResults<DataverseItem>>> type = new ParameterizedTypeReference<DataverseResponse<SearchResults<DataverseItem>>>() {
		};
		ResponseEntity<DataverseResponse<SearchResults<DataverseItem>>> resp = template.exchange(url, HttpMethod.GET, createHttpEntity(headers), type);
		log.debug(resp.getBody().getData().toString());
		return resp.getBody();
	}

	private void validateSrchConfig(SearchConfig cfg, SearchType expected) {
		if (cfg.getType().size() != 1 || !cfg.getType().contains(expected)) {
			throw new IllegalArgumentException(String.format("Search must be configured to search only  %ss", expected.name()));
		}
	}
}
