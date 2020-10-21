//******************************************************************************
//                          DataModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import javax.jdo.query.BooleanExpression;
import org.opensilex.nosql.model.NoSQLModel;
import org.opensilex.server.rest.validation.DateFormat;

/**
 *
 * @author sammy
 */
@PersistenceCapable(table = "Data")
@Index(name="Unicity", unique="true", members={"variable", "scientificObjects","provenanceURI","date"})
public class DataModel implements NoSQLModel{

    @NotPersistent
    private final String baseURI = "id/data";
    @NotPersistent
    private String[] URICompose;
    
    @Persistent
    @PrimaryKey
    private URI uri;
    
    @Element(embeddedMapping={
    @Embedded(members={
        @Persistent(name="uri", column="uri"),
        @Persistent(name="type", column="rdf:type")})
    })
    @Join(column="uri")
    @Persistent(defaultFetchGroup="true")

    private List<ProvEntityModel> scientificObjects;
    
    private URI variable;
    
    private URI provenanceURI;
    
    @Persistent(defaultFetchGroup="true")    
    Map provenanceSettings; 
    
    @Element(embeddedMapping={
    @Embedded(members={
        @Persistent(name="uri", column="uri"),
        @Persistent(name="type", column="rdf:type")})
    })
    @Join(column="uri")
    @Persistent(defaultFetchGroup="true")
    @Column(name="prov:used")
    List<ProvEntityModel> provUsed;
    
    private ZonedDateTime date;
    
    private String timezone;
    
    @Persistent(defaultFetchGroup="true")
    private Object value;
    
    private Float confidence = null;
    
    @Persistent(defaultFetchGroup="true")
    private Map metadata;  
    
    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void setObject(List<ProvEntityModel> objects){
        this.scientificObjects = objects;
    }
    
    public void setVariable(URI variable){
        this.variable = variable;
    }
        
    public void setDate(String date) throws ParseException{
        if(date != null){
            DateFormat[] formats = {DateFormat.YMDTHMSZ, DateFormat.YMDTHMSMSZ};
            ZonedDateTime zdt = null;
            for (DateFormat dateCheckFormat : formats) {
                try { 
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateCheckFormat.toString());
                    zdt = ZonedDateTime.parse(date, dtf);
                    break;
                } catch (DateTimeParseException e) {
                }                    
            }
            this.date = zdt;
            this.timezone = this.date.getZone().toString();
        }      
           
    }
    
    public void setDate(ZonedDateTime date){
        if(date != null){   
            this.date = date;
            this.timezone = this.date.getZone().toString();
        }
    }
    
    public void setTimezone(String tz){
        this.timezone = tz;
    }
    
    public void setValue(Object value){
        this.value = value;
    }
    
    public void setConfidence(Float c){
        this.confidence = c;
    }
    
    public void setURICompose(String[] elt){
        this.URICompose = elt;
    }
    
    @Override
    public URI getUri() {
        return uri;
    }
    
    public List<ProvEntityModel> getObject(){
        return scientificObjects;
    }
    
    public URI getVariable(){
        return variable;
    }
        
    public ZonedDateTime getDate(){
        if (date == null) return null;
        
        ZonedDateTime zdt = date;
        return zdt.withZoneSameInstant(ZoneId.of(timezone));
    }
    
    public Object getValue(){
        return value;
    }
    
    public Float getConfidence(){
        return confidence;
    }
    
    public Map getMetadata(){
        return metadata;
    }
    
    public void setMetadata(Map m){
        this.metadata = m;
    }

    public URI getProvenanceURI() {
        return provenanceURI;
    }

    public void setProvenanceURI(URI provenanceURI) {
        this.provenanceURI = provenanceURI;
    }

    public Map getProvenanceSettings() {
        return provenanceSettings;
    }

    public void setProvenanceSettings(Map provenanceSettings) {
        this.provenanceSettings = provenanceSettings;
    }
    
    public List<ProvEntityModel> getProvUsed() {
        return provUsed;
    }

    public void setProvUsed(List<ProvEntityModel> provUsed) {
        this.provUsed = provUsed;
    }
    

    @Override
    public <T extends NoSQLModel> T update(T instance) {
        
        DataModel newInstance =  new DataModel();
        DataModel updateInstance = (DataModel) instance;

        newInstance.setUri(instance.getUri());
        if(updateInstance.getObject() !=  null)
            newInstance.setObject(updateInstance.getObject());
        else
            newInstance.setObject(scientificObjects);

        if(updateInstance.getVariable() != null)
            newInstance.setVariable(updateInstance.getVariable());
        else
            newInstance.setVariable(variable);

        if(updateInstance.getProvenanceURI()!=  null)
            newInstance.setProvenanceURI(updateInstance.getProvenanceURI());
        else
            newInstance.setProvenanceURI(provenanceURI);
        
        if(updateInstance.getProvenanceSettings()!=  null)
            newInstance.setProvenanceSettings(updateInstance.getProvenanceSettings());
        else
            newInstance.setProvenanceSettings(provenanceSettings);
        
        if(updateInstance.getProvUsed() !=  null)
            newInstance.setProvUsed(updateInstance.getProvUsed());
        else
            newInstance.setProvUsed(provUsed);
        
        if(updateInstance.getDate() !=  null)
            newInstance.setDate(updateInstance.getDate());
        else
            newInstance.setDate(getDate());

        if(updateInstance.getValue() !=  null)
            newInstance.setValue(updateInstance.getValue());
        else
            newInstance.setValue(value);

        newInstance.setConfidence(updateInstance.getConfidence());

        if(updateInstance.getMetadata() !=  null)
            newInstance.setMetadata(updateInstance.getMetadata());
        else
            newInstance.setMetadata(metadata);  

        return (T) newInstance;
        
    }

    @Override
    public String getGraphPrefix() {
        return baseURI;
    }

    @Override
    public String[] getUriSegments(NoSQLModel instance) {
        /*URICompose = Arrays.stream(URICompose).filter(Objects::nonNull).toArray(String[]::new);
        return ArrayUtils.addAll(URICompose,new String[]{formatDateURI()});*/
        return new String[]{
            Timestamp.from(ZonedDateTime.now().toInstant()).toString(),
            Timestamp.from(getDate().toInstant()).toString()
        };
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
