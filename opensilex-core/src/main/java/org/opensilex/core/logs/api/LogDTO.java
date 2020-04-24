////******************************************************************************
////                                DataSearchDTO.java
//// SILEX-PHIS
//// Copyright © INRAE 2020
//// Creation date: February 2020
//// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
////******************************************************************************
//package org.opensilex.core.logs.api;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import opensilex.service.resource.dto.data.AccessUserLogDTO;
//import org.bson.Document;
//
///**
// * Data Query Log SearchDTO.
// * @author Arnaud Charleroy
// */
//public class LogDTO{
//    //URI of the data
//    //@example http://www.sunagri.fr/sunagri/id/agent/admin_sunagri 
//    private final AccessUserLogDTO user;
//    //Remote address of the user query
//    private final String remoteIpAddress; 
//    //Variable of the data 
//    private final Document userQuery;
//    //Date of the data. The format should be yyyy-MM-ddTHH:mm:ssZ
//    //@example 2017-05-21 23:51:00.000Z
//    private final String date; 
//    
//    @JsonIgnore
//    private static final DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
//
//     public LogDTO(AccessUserLogDTO user, Document query, LocalDate date,String remoteAddress ) {
//        this.user = user;
//        this.userQuery = query;
//        
//        this.date = formatter.format(date);
//        this.remoteIpAddress = remoteAddress; 
//    }
//   
//}
