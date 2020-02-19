//******************************************************************************
//                                DataSearchDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 21 mai 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import opensilex.service.configuration.DateFormat;
import opensilex.service.model.User;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import org.bson.Document;

/**
 * Data search DTO.
 * @author Arnaud Charleroy
 */
public class DataQueryLogSearchDTO extends AbstractVerifiedClass {
    //URI of the data
    //@example http://www.sunagri.fr/sunagri/id/agent/admin_sunagri 
    protected User user;
    //Remote address of the user query
    protected String remoteIpAddress; 
    //Variable of the data 
    protected Document userQuery;
    //Date of the data. The format should be yyyy-MM-ddTHH:mm:ssZ
    //@example 2017-05-21 23:51:00.000Z
    protected String date; 

     public DataQueryLogSearchDTO(User user, Document query, Date date,String remoteAddress ) {
        this.user = user;
        this.userQuery = query;
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        this.date = df.format(date); 
        this.remoteIpAddress = remoteAddress; 
    }
    
    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
