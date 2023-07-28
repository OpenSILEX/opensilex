// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /** <pre>
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
 * Encapsulates search results.
 * <p/>
 * Items' subclass can be identified from the 'type' value.
 * 
 * @author rspace
 */
public class SearchResults<T extends Item> {
	String q;
	@JsonProperty("total_count")
	private int totalCount;
	@JsonProperty("count_in_response")
	private int countInResponse;
	int start;
	List<Object> spellingAlternatives;
	List<T> items;

	/**
	 * Filters a list of SearchHits by their type.
	 * @param type
	 * @return
	 */
	public List<Item> filterByType(SearchType type) {
		return items.stream().filter(i -> i.getType().equalsIgnoreCase(type.name())).collect(Collectors.toList());
	}

	@SuppressWarnings("all")
	public SearchResults() {
	}

	@SuppressWarnings("all")
	public String getQ() {
		return this.q;
	}

	@SuppressWarnings("all")
	public int getTotalCount() {
		return this.totalCount;
	}

	@SuppressWarnings("all")
	public int getCountInResponse() {
		return this.countInResponse;
	}

	@SuppressWarnings("all")
	public int getStart() {
		return this.start;
	}

	@SuppressWarnings("all")
	public List<Object> getSpellingAlternatives() {
		return this.spellingAlternatives;
	}

	@SuppressWarnings("all")
	public List<T> getItems() {
		return this.items;
	}

	@SuppressWarnings("all")
	public void setQ(final String q) {
		this.q = q;
	}

	@JsonProperty("total_count")
	@SuppressWarnings("all")
	public void setTotalCount(final int totalCount) {
		this.totalCount = totalCount;
	}

	@JsonProperty("count_in_response")
	@SuppressWarnings("all")
	public void setCountInResponse(final int countInResponse) {
		this.countInResponse = countInResponse;
	}

	@SuppressWarnings("all")
	public void setStart(final int start) {
		this.start = start;
	}

	@SuppressWarnings("all")
	public void setSpellingAlternatives(final List<Object> spellingAlternatives) {
		this.spellingAlternatives = spellingAlternatives;
	}

	@SuppressWarnings("all")
	public void setItems(final List<T> items) {
		this.items = items;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof SearchResults)) return false;
		final SearchResults<?> other = (SearchResults<?>) o;
		if (!other.canEqual((Object) this)) return false;
		if (this.getTotalCount() != other.getTotalCount()) return false;
		if (this.getCountInResponse() != other.getCountInResponse()) return false;
		if (this.getStart() != other.getStart()) return false;
		final Object this$q = this.getQ();
		final Object other$q = other.getQ();
		if (this$q == null ? other$q != null : !this$q.equals(other$q)) return false;
		final Object this$spellingAlternatives = this.getSpellingAlternatives();
		final Object other$spellingAlternatives = other.getSpellingAlternatives();
		if (this$spellingAlternatives == null ? other$spellingAlternatives != null : !this$spellingAlternatives.equals(other$spellingAlternatives)) return false;
		final Object this$items = this.getItems();
		final Object other$items = other.getItems();
		if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof SearchResults;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.getTotalCount();
		result = result * PRIME + this.getCountInResponse();
		result = result * PRIME + this.getStart();
		final Object $q = this.getQ();
		result = result * PRIME + ($q == null ? 43 : $q.hashCode());
		final Object $spellingAlternatives = this.getSpellingAlternatives();
		result = result * PRIME + ($spellingAlternatives == null ? 43 : $spellingAlternatives.hashCode());
		final Object $items = this.getItems();
		result = result * PRIME + ($items == null ? 43 : $items.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "SearchResults(q=" + this.getQ() + ", totalCount=" + this.getTotalCount() + ", countInResponse=" + this.getCountInResponse() + ", start=" + this.getStart() + ", spellingAlternatives=" + this.getSpellingAlternatives() + ", items=" + this.getItems() + ")";
	}
}
