package org.opensilex.sparql.service;

import java.net.URI;

public record SPARQLLiteral(String value, String lang, URI datatype) {
}
