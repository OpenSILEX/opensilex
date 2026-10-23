package org.opensilex.core.widget.dal;

import org.opensilex.sparql.service.SearchFilter;

/**
 * Search criteria of the widget concept. Use a filter object as soon as a search has three or more criteria; the base
 * class already carries the language, the ordering and the pagination.
 */
public class WidgetSearchFilter extends SearchFilter {

    private String namePattern;

    public WidgetSearchFilter() {
        super();
    }

    public String getNamePattern() {
        return namePattern;
    }

    public WidgetSearchFilter setNamePattern(String namePattern) {
        this.namePattern = namePattern;
        return this;
    }
}
