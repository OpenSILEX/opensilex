/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.update;

import java.time.LocalDate;

/**
 *
 * @author vincent
 */
public interface Update {
    
    public LocalDate getDate();
    
    public String getDescription();
    
    public void execute() throws Exception;
}
