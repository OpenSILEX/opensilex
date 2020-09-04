/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.dal;

import java.net.URI;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.query.BooleanExpression;
import org.apache.commons.lang3.ArrayUtils;
import org.opensilex.nosql.model.NoSQLModel;

/**
 *
 * @author sammy
 */
@PersistenceCapable(table = "Data")
public class DataModel implements NoSQLModel{

    @NotPersistent
    private final String baseURI = "id/data";
    @NotPersistent
    private String[] URICompose;
    
    @Persistent
    private URI uri;
    
    @Persistent
    private URI object;
    
    @Persistent
    private URI variable;
    
    @Persistent
    private URI provenance;
    
    @Persistent
    private ZonedDateTime date;
    
    @Persistent
    private String timezone;
    
    @Persistent
    private Object value;
    
    @Persistent
    private int confidence;
    
    /*@Persistent
    private Object metaData;*/
    
    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void setObject(URI object){
        this.object = object;
    }
    
    public void setVariable(URI variable){
        this.variable = variable;
    }
    
    public void setProvenance(URI provenance){
        this.provenance = provenance;
    }
    
    public void setDate(String date) throws ParseException{
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXXX");
        this.date = ZonedDateTime.parse(date,dtf);
        
        this.timezone = this.date.getZone().toString();
    }
    
    public void setDate(ZonedDateTime date){
        this.date = date;
        this.timezone = this.date.getZone().toString();
    }
    
    public void setTimezone(String tz){
        this.timezone = tz;
    }
    
    public void setValue(Object value){
        this.value = value;
    }
    
    public void setConfidence(int c){
        this.confidence = c;
    }
    
    public void setURICompose(String[] elt){
        this.URICompose = elt;
    }
    
    @Override
    public URI getUri() {
        return uri;
    }
    
    public URI getObject(){
        return object;
    }
    
    public URI getVariable(){
        return variable;
    }
    
    public URI getProvenance(){
        return provenance;
    }
     
    public ZonedDateTime getDate(){
        return date;
    }
    
    public Object getValue(){
        return value;
    }
    
    public int getConfidence(){
        return confidence;
    }
    
    /*public Object getMetaData(){
        return metaData;
    }
    
    @Override
    public void setMetaData(Object m){
        this.metaData = m;
    }*/

    @Override
    public <T extends NoSQLModel> T update(T instance) {
        
        DataModel newInstance =  new DataModel();
        DataModel updateInstance = (DataModel) instance;

        newInstance.setUri(instance.getUri());
        if(updateInstance.getObject() !=  null)
            newInstance.setObject(updateInstance.getObject());
        else
            newInstance.setObject(object);

        if(updateInstance.getVariable() != null)
            newInstance.setVariable(updateInstance.getVariable());
        else
            newInstance.setVariable(variable);

        if(updateInstance.getProvenance() !=  null)
            newInstance.setProvenance(updateInstance.getProvenance());
        else
            newInstance.setProvenance(provenance);

        if(updateInstance.getDate() !=  null)
            newInstance.setDate(updateInstance.getDate());
        else
            newInstance.setDate(getDate());

        if(updateInstance.getValue() !=  null)
            newInstance.setValue(updateInstance.getValue());
        else
            newInstance.setValue(value);

        newInstance.setConfidence(updateInstance.getConfidence());

       /* if(updateInstance.getMetaData() !=  null)
            newInstance.setMetaData(updateInstance.getMetaData());
        else
            newInstance.setMetaData(metaData);*/

        return (T) newInstance;
        
    }

    @Override
    public String getGraphPrefix() {
        return baseURI;
    }

    @Override
    public String[] getUriSegments(NoSQLModel instance) { 
        return ArrayUtils.addAll(URICompose,new String[]{formatDateURI()});
    }
    
    @Override
    public String toString(){
        String str = "";
        if(object != null) str += object.toString();
        if(variable != null) str += variable.toString();
        if(provenance != null) str+= provenance.toString();
        if(date != null) str += date.toString();
        return str ;
    }
    
    @Override
    public BooleanExpression getURIExpr(URI uri){
        QDataModel candidate = QDataModel.candidate();
        return candidate.uri.eq(uri);
    }
    
    private String formatDateURI(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String sd = dtf.format(date);
        return sd;
    }
}
