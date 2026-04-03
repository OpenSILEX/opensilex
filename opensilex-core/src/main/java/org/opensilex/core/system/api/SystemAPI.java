//******************************************************************************
//                          SystemAPI.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.system.api;

import io.swagger.annotations.*;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import org.opensilex.server.response.ErrorResponse;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Properties;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.ServerModule;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents System API ressources
 * @author Arnaud Charleroy
 */
@Api(SystemAPI.CREDENTIAL_SYSTEM_GROUP_ID)
@Path("/core/system")
public class SystemAPI {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(SystemAPI.class);
    public static final String CREDENTIAL_SYSTEM_GROUP_ID = "System";
    
    @Inject
    private ServerModule serverModule;
    
    @CurrentUser
    AccountModel user;

    @GET
    @Path("/info")
    @ApiOperation("get system information")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Return API version info", response = VersionInfoDTO.class),
        @ApiResponse(code = 500, message = "Internal Server Error", response = ErrorResponse.class)
    })
    public Response getVersionInfo() throws Exception { 
        VersionInfoDTO versionInfoDTO = new VersionInfoDTO();   
        
        // title
        versionInfoDTO.setTitle(this.serverModule.getOpenSilex().getSystemConfig().instanceTitle());

        // version
        versionInfoDTO.setVersion(this.serverModule.getOpenSilexVersion()); 
        
         // Add version in list for all modules
        List<ApiModulesInfo> modulesVersion = new ArrayList<>();
        LOGGER.debug("List loaded modules");
        this.serverModule.getOpenSilex().getModules().forEach((OpenSilexModule module) -> {
            LOGGER.debug(module.getClass().getSimpleName() + "-" + module.getMavenProperties().toString());
            modulesVersion.add(new ApiModulesInfo(module.getClass().getCanonicalName(),module.getOpenSilexVersion()));
        });
        versionInfoDTO.setModulesVersion(modulesVersion); 
        
        // description
        versionInfoDTO.setDescription(this.serverModule.getOpenSilex().getSystemConfig().instanceDescription());
        
        // contact
        versionInfoDTO.setContact(
            new ApiContactInfoDTO(
                this.serverModule.getOpenSilex().getSystemConfig().contactName(), 
                this.serverModule.getOpenSilex().getSystemConfig().contactEmail(),
                new URL(this.serverModule.getOpenSilex().getSystemConfig().projectHomepage())
            )
        );
        
        // license
        versionInfoDTO.setLicense(
                new ApiLicenseInfoDTO(
                    "GNU Affero General Public License v3",
                    "https://www.gnu.org/licenses/agpl-3.0.fr.html"
                )
        );
        
        // external docs
        versionInfoDTO.setExternalDocs(
            new ApiExternalDocsDTO(
                "Opensilex dev documentation",
                "https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/index.md"
            )
        );
        

        // api docs
        versionInfoDTO.setApiDocs( 
            new ApiExternalDocsDTO(
                "Opensilex API documentation",
                this.serverModule.getBaseURL() + "api-docs"
            )
        );
        
        versionInfoDTO.setGithubPage(
        "https://github.com/OpenSILEX/opensilex"
        );
        
        ApiGitCommitDTO gitInfo = null;
        
        try {
            File gitPropertiesFile = ClassUtils.getFileFromClassArtifact(OpenSilex.class, "git.properties");

            Properties gitProperties = new Properties();
            gitProperties.load(new FileReader(gitPropertiesFile));

            String gitCommitFull = gitProperties.getProperty("git.commit.id.full", null);
            String gitCommitMessage = gitProperties.getProperty("git.commit.message.full", null);  

            
            gitInfo = new ApiGitCommitDTO(gitCommitFull,gitCommitMessage);
        } catch (Exception ex) {
            System.out.println("No git commit information found");
            LOGGER.debug("Exception raised:", ex);
        }
        
        versionInfoDTO.setGitCommit(gitInfo);
        
        
        return new SingleObjectResponse<>(versionInfoDTO).getResponse();
    }
    
}
