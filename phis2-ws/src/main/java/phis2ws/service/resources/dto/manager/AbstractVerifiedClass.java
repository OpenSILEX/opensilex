//**********************************************************************************************
//                                       AbstractVerifiedClass.java 
//
// Author(s): Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: may 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: A class which contains methods to verify automatically class's attributes from rules defined by user
//***********************************************************************************************
package phis2ws.service.resources.dto.manager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Réprésente un classe qui permet de définir des objets qui peuvent vérifier leur attributs
 * @author Arnaud CHARLEROY
 */
public abstract class AbstractVerifiedClass implements VerifiedClassInterface {
    /**
     * Retourne une map contenant l'état dans la clé "state" et les champs manquant de l'objet
     * @return Map
     */
    @Override
    public Map<String,Object> isOk() {
        Map<String,Object> ok = new HashMap<>();
        Map<String,Boolean> rules = this.rules();
       
        Field [] attributes =  this.getClass().getDeclaredFields();
        
        Boolean validationBool = Boolean.TRUE;
        
        try {
            for (Field field : attributes) {
                field.setAccessible(true);
                Object fieldObject = field.get(this);
                if (fieldObject instanceof List) {
                    List list = (List) ((List) fieldObject);
                    if(!list.isEmpty() 
                            && Objects.equals(rules.get(field.getName()), Boolean.TRUE)){
                        Map<String,Object> verifiedClassInstance = ((AbstractVerifiedClass) list.get(0)).isOk();
                        if(verifiedClassInstance.get("state") == Boolean.FALSE){
                            validationBool = Boolean.FALSE; 
                            verifiedClassInstance.remove("state");
                            ok.put(field.getName(), verifiedClassInstance);
                        }
                    }
                    
                } else if (fieldObject instanceof AbstractVerifiedClass) {
                    Map<String,Object> verifiedClassInstance = ((AbstractVerifiedClass) fieldObject).isOk();
                    if (verifiedClassInstance.get("state") == Boolean.FALSE) {
                        validationBool = Boolean.FALSE; 
                        verifiedClassInstance.remove("state");
                        ok.put(field.getName(), verifiedClassInstance);
                    }
                } else {
                    if (Objects.equals(rules.get(field.getName()), Boolean.TRUE) && 
                        (Objects.equals(fieldObject, null) || (fieldObject instanceof String && fieldObject == "")) ) {
                        validationBool = Boolean.FALSE; 
                        ok.put(field.getName(), "empty");
                    }
                }
            }
        } catch(SecurityException | IllegalArgumentException | IllegalAccessException ex){
            ok.replace("state", Boolean.FALSE); 
        }
        ok.put("state", validationBool); 
        return ok;
    }
    
    /**
     * Permet de transformer un objet en HashMap .
     * Fonction pratique pour la transformation en JSON
     * @return 
     */
    public Map<String,String> toHashMap() {
        Map<String,String> objectHashMap = new LinkedHashMap<>();
        Field [] attributes =  this.getClass().getDeclaredFields();
        try{
            for (Field field : attributes) {
                field.setAccessible(true);
                objectHashMap.put(field.getName(), (String) field.get(this));
            }
        }catch(SecurityException | IllegalArgumentException | IllegalAccessException ex){
            objectHashMap.put("convertError", ex.getMessage());
        }
        return objectHashMap;
    }
    
    
}
