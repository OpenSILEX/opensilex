////******************************************************************************
////                               AccessUserLogDTO.java 
//// SILEX-PHIS
//// Copyright © INRAE 2020
//// Creation date: February 2020
//// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
////******************************************************************************
//package opensilex.service.resource.dto.data;
//
//import io.swagger.annotations.ApiModelProperty;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//
///**
// * DataLogAccessUser DTO.
//* @author Arnaud Charleroy
// */
//public class AccessUserLogDTO  {
//
//    final static Logger LOGGER = LoggerFactory.getLogger(AccessUserLogDTO.class);
//    
//    private String email;
//    private String firstName;
//    private String familyName;
//    private String uri;
//
//    public AccessUserLogDTO(String email, String firstName, String familyName, String uri) {
//        this.email = email;
//        this.firstName = firstName;
//        this.familyName = familyName;
//        this.uri = uri;
//    }
//    
//  
//     
//    @ApiModelProperty()
//    public String getEmail() {
//        return email;
//    }
//
//    public void setEmail(String email) {
//        this.email = email;
//    }
//    
//    @ApiModelProperty()
//    public String getFirstName() {
//        return firstName;
//    }
//
//    public void setFirstName(String firstName) {
//        this.firstName = firstName;
//    }
//    
//    @ApiModelProperty()
//    public String getFamilyName() {
//        return familyName;
//    }
//
//    public void setFamilyName(String familyName) {
//        this.familyName = familyName;
//    }
//    
//    @ApiModelProperty()
//    public String getUri() {
//        return uri;
//    }
//
//    public void setUri(String uri) {
//        this.uri = uri;
//    }
//}