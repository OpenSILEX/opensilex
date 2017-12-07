/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.view.brapi.form;

import phis2ws.service.view.brapi.Metadata;

/**
 *
 * @author charlero
 */
public class ResponseUnique {
    Metadata metadata;
    Object result;

    public ResponseUnique( Object result) {
        this.metadata = new Metadata();
        this.result = result;
    }
    
    
}
