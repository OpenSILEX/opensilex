/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.resources.dto.manager;

import java.util.Map;


public interface VerifiedClassInterface<T> {
    
    public Map isOk();
    
//    public Map rules();
    
    public T createObjectFromDTO();
}
