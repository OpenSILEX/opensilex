package org.opensilex.core.event.dal;

import org.opensilex.sparql.service.SearchFilter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

public class EventSearchFilter extends SearchFilter {

    protected String target;
    protected List<URI> targets;
    protected URI baseType;
    protected String descriptionPattern;
    protected URI type;
    protected OffsetDateTime start;
    protected OffsetDateTime end;

    public String getTarget() {
        return target;
    }

    public EventSearchFilter setTarget(String target) {
        this.target = target;
        return this;
    }

    public List<URI> getTargets() {
        return targets;
    }

    public EventSearchFilter setTargets(List<URI> targets) {
        this.targets = targets;
        return this;
    }

    public URI getBaseType() {
        return baseType;
    }

    public EventSearchFilter setBaseType(URI baseType) {
        this.baseType = baseType;
        return this;
    }

    public String getDescriptionPattern() {
        return descriptionPattern;
    }

    public EventSearchFilter setDescriptionPattern(String descriptionPattern) {
        this.descriptionPattern = descriptionPattern;
        return this;
    }

    public URI getType() {
        return type;
    }

    public EventSearchFilter setType(URI type) {
        this.type = type;
        return this;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public EventSearchFilter setStart(OffsetDateTime start) {
        this.start = start;
        return this;
    }

    public OffsetDateTime getEnd() {
        return end;

    }

    public EventSearchFilter setEnd(OffsetDateTime end) {
        this.end = end;
        return this;
    }
}
