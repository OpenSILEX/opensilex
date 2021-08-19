//******************************************************************************
//                        ApiGitCommitDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensilex.core.system.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Class that represents informations about lastest commit
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"commit_id", "commit_message"})
public class ApiGitCommitDTO {
    
    @JsonProperty("commit_id")
    private String commitId;
    
    @JsonProperty("commit_message")
    private String commitMessage;

    public ApiGitCommitDTO(String commitId, String commitMessage) {
        this.commitId = commitId;
        this.commitMessage = commitMessage;
    }
    
    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }
}
