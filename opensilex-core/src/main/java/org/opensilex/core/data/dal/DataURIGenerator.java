/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.dal;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.valuegenerator.ValueGenerationBlock;
import org.datanucleus.store.valuegenerator.AbstractGenerator;

/**
 *
 * @author sammy
 */
public class DataURIGenerator extends AbstractGenerator {
    
    public DataURIGenerator(String name){
        super(null,name);
    }

    /**
     * Method to reserve "size" ValueGenerations to the ValueGenerationBlock.
     * @param size The block size
     * @return The reserved blo
     */
    @Override
    public ValueGenerationBlock reserveBlock(long size)
    {
        Object[] ids = new Object[(int) size];
        for (int i = 0; i < size; i++)
        {
            ids[i] = getIdentifier();
            
        }
        return new ValueGenerationBlock(ids);
    }

    /**
     * Create a UUID identifier.
     * @return The identifier
     */
    private String getIdentifier()
    {
        Instant instant = Instant.now();
        long timeStampMillis = instant.toEpochMilli();
        return "http://opensilex.dev/" + "id/data/" + Long.toString(timeStampMillis);
    }
}
