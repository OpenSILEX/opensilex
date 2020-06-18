package org.opensilex.core.ontology;

import java.net.URI;
import java.util.List;

public interface SKOSReferences {

    List<URI> getExactMatch();

    void setExactMatch(List<URI> exactMatch);

    List<URI> getCloseMatch();

    void setCloseMatch(List<URI> closeMatch);

    List<URI> getBroader();

    void setBroader(List<URI> broader);

    List<URI> getNarrower();

    void setNarrower(List<URI> narrower);
}
