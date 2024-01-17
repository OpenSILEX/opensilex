package org.opensilex.utils;

import java.net.URI;
import java.util.List;

public class ExcludableUriList {
    public boolean excludeResults;
    public List<URI> result;

    public ExcludableUriList(boolean excludeResults, List<URI> result){
        this.result = result;
        this.excludeResults = excludeResults;
    }
}
