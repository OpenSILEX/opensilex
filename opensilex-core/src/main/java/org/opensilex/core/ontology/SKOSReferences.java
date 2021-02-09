package org.opensilex.core.ontology;

import java.net.URI;
import java.util.List;

public interface SKOSReferences {

    List<URI> getExactMatch();

    void setExactMatch(List<URI> exactMatch);

    List<URI> getCloseMatch();

    void setCloseMatch(List<URI> closeMatch);

    List<URI> getBroadMatch();

    void setBroadMatch(List<URI> broadMatch);

    List<URI> getNarrowMatch();

    void setNarrowMatch(List<URI> narrowMatch);
}
