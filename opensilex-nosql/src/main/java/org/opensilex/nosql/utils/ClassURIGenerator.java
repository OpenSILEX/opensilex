/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.utils;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author sammy
 */
public interface ClassURIGenerator<T> extends URIGenerator<T> {

    @Override
    public default String getInstanceURI(T instance) throws UnsupportedEncodingException  {
        return URIGenerator.normalize(getUriSegments(instance));
    }

    public String[] getUriSegments(T instance);
}
