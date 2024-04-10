package org.opensilex.utils.pagination;


/**
 * Define the strategy to use for counting element before running the search
 */
public enum PaginatedSearchStrategy {

    /**
     * Only check if there exists a next page during the search.
     * This
     */
    HAS_NEXT_PAGE,

    /**
     * Run a count query before the search
     */
    COUNT_QUERY_BEFORE_SEARCH
}
