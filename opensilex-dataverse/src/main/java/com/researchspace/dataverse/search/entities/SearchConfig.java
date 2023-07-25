// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.search.entities;

import org.apache.commons.lang3.Validate;
import java.util.EnumSet;

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
 * Read-only search configuration object. <br/>
 * Use the <code>builder()</code> method to return a new SearchConfigBuilder to
 * build a search configuration in a Fluent style.
 * 
 * @author rspace
 */
public final class SearchConfig {

	/**
	 * Builder for configuring search via fluent API
	 * 
	 * @author rspace
	 */
	public static class SearchConfigBuilder {
		@SuppressWarnings("all")
		private EnumSet<SearchType> type;
		@SuppressWarnings("all")
		private String q;
		@SuppressWarnings("all")
		private String subtree;
		@SuppressWarnings("all")
		private String filterQuery;
		@SuppressWarnings("all")
		private SortBy sortBy;
		@SuppressWarnings("all")
		private SortOrder sortOrder;
		@SuppressWarnings("all")
		private int perPage;
		@SuppressWarnings("all")
		private int start;
		@SuppressWarnings("all")
		private boolean showRelevance;
		@SuppressWarnings("all")
		private boolean showFacets;

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

		@SuppressWarnings("all")
		SearchConfigBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder type(final EnumSet<SearchType> type) {
			this.type = type;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder q(final String q) {
			if (q == null) {
				throw new NullPointerException("q is marked non-null but is null");
			}
			this.q = q;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder subtree(final String subtree) {
			this.subtree = subtree;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder filterQuery(final String filterQuery) {
			this.filterQuery = filterQuery;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder sortBy(final SortBy sortBy) {
			this.sortBy = sortBy;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder sortOrder(final SortOrder sortOrder) {
			this.sortOrder = sortOrder;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder showRelevance(final boolean showRelevance) {
			this.showRelevance = showRelevance;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public SearchConfigBuilder showFacets(final boolean showFacets) {
			this.showFacets = showFacets;
			return this;
		}

		@SuppressWarnings("all")
		public SearchConfig build() {
			return new SearchConfig(this.type, this.q, this.subtree, this.filterQuery, this.sortBy, this.sortOrder, this.perPage, this.start, this.showRelevance, this.showFacets);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "SearchConfig.SearchConfigBuilder(type=" + this.type + ", q=" + this.q + ", subtree=" + this.subtree + ", filterQuery=" + this.filterQuery + ", sortBy=" + this.sortBy + ", sortOrder=" + this.sortOrder + ", perPage=" + this.perPage + ", start=" + this.start + ", showRelevance=" + this.showRelevance + ", showFacets=" + this.showFacets + ")";
		}
	}

	public static final int MAX_RESULTS_PER_PAGE = 1000;
	private final EnumSet<SearchType> type;
	private final String q;
	private final String subtree;
	private final String filterQuery;
	private final SortBy sortBy;
	private final SortOrder sortOrder;
	private final int perPage;
	private final int start;
	private final boolean showRelevance;
	private final boolean showFacets;

	@SuppressWarnings("all")
	SearchConfig(final EnumSet<SearchType> type, final String q, final String subtree, final String filterQuery, final SortBy sortBy, final SortOrder sortOrder, final int perPage, final int start, final boolean showRelevance, final boolean showFacets) {
		if (q == null) {
			throw new NullPointerException("q is marked non-null but is null");
		}
		this.type = type;
		this.q = q;
		this.subtree = subtree;
		this.filterQuery = filterQuery;
		this.sortBy = sortBy;
		this.sortOrder = sortOrder;
		this.perPage = perPage;
		this.start = start;
		this.showRelevance = showRelevance;
		this.showFacets = showFacets;
	}

	@SuppressWarnings("all")
	public static SearchConfigBuilder builder() {
		return new SearchConfigBuilder();
	}

	@SuppressWarnings("all")
	public SearchConfigBuilder toBuilder() {
		return new SearchConfigBuilder().type(this.type).q(this.q).subtree(this.subtree).filterQuery(this.filterQuery).sortBy(this.sortBy).sortOrder(this.sortOrder).perPage(this.perPage).start(this.start).showRelevance(this.showRelevance).showFacets(this.showFacets);
	}

	@SuppressWarnings("all")
	public EnumSet<SearchType> getType() {
		return this.type;
	}

	@SuppressWarnings("all")
	public String getQ() {
		return this.q;
	}

	@SuppressWarnings("all")
	public String getSubtree() {
		return this.subtree;
	}

	@SuppressWarnings("all")
	public String getFilterQuery() {
		return this.filterQuery;
	}

	@SuppressWarnings("all")
	public SortBy getSortBy() {
		return this.sortBy;
	}

	@SuppressWarnings("all")
	public SortOrder getSortOrder() {
		return this.sortOrder;
	}

	@SuppressWarnings("all")
	public int getPerPage() {
		return this.perPage;
	}

	@SuppressWarnings("all")
	public int getStart() {
		return this.start;
	}

	@SuppressWarnings("all")
	public boolean isShowRelevance() {
		return this.showRelevance;
	}

	@SuppressWarnings("all")
	public boolean isShowFacets() {
		return this.showFacets;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof SearchConfig)) return false;
		final SearchConfig other = (SearchConfig) o;
		if (this.getPerPage() != other.getPerPage()) return false;
		if (this.getStart() != other.getStart()) return false;
		if (this.isShowRelevance() != other.isShowRelevance()) return false;
		if (this.isShowFacets() != other.isShowFacets()) return false;
		final Object this$type = this.getType();
		final Object other$type = other.getType();
		if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
		final Object this$q = this.getQ();
		final Object other$q = other.getQ();
		if (this$q == null ? other$q != null : !this$q.equals(other$q)) return false;
		final Object this$subtree = this.getSubtree();
		final Object other$subtree = other.getSubtree();
		if (this$subtree == null ? other$subtree != null : !this$subtree.equals(other$subtree)) return false;
		final Object this$filterQuery = this.getFilterQuery();
		final Object other$filterQuery = other.getFilterQuery();
		if (this$filterQuery == null ? other$filterQuery != null : !this$filterQuery.equals(other$filterQuery)) return false;
		final Object this$sortBy = this.getSortBy();
		final Object other$sortBy = other.getSortBy();
		if (this$sortBy == null ? other$sortBy != null : !this$sortBy.equals(other$sortBy)) return false;
		final Object this$sortOrder = this.getSortOrder();
		final Object other$sortOrder = other.getSortOrder();
		if (this$sortOrder == null ? other$sortOrder != null : !this$sortOrder.equals(other$sortOrder)) return false;
		return true;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.getPerPage();
		result = result * PRIME + this.getStart();
		result = result * PRIME + (this.isShowRelevance() ? 79 : 97);
		result = result * PRIME + (this.isShowFacets() ? 79 : 97);
		final Object $type = this.getType();
		result = result * PRIME + ($type == null ? 43 : $type.hashCode());
		final Object $q = this.getQ();
		result = result * PRIME + ($q == null ? 43 : $q.hashCode());
		final Object $subtree = this.getSubtree();
		result = result * PRIME + ($subtree == null ? 43 : $subtree.hashCode());
		final Object $filterQuery = this.getFilterQuery();
		result = result * PRIME + ($filterQuery == null ? 43 : $filterQuery.hashCode());
		final Object $sortBy = this.getSortBy();
		result = result * PRIME + ($sortBy == null ? 43 : $sortBy.hashCode());
		final Object $sortOrder = this.getSortOrder();
		result = result * PRIME + ($sortOrder == null ? 43 : $sortOrder.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "SearchConfig(type=" + this.getType() + ", q=" + this.getQ() + ", subtree=" + this.getSubtree() + ", filterQuery=" + this.getFilterQuery() + ", sortBy=" + this.getSortBy() + ", sortOrder=" + this.getSortOrder() + ", perPage=" + this.getPerPage() + ", start=" + this.getStart() + ", showRelevance=" + this.isShowRelevance() + ", showFacets=" + this.isShowFacets() + ")";
	}
}
