/*
 * 
 */
package com.researchspace.dataverse.api.v1;

import com.researchspace.dataverse.entities.DataverseResponse;
import com.researchspace.dataverse.entities.DvMessage;
/**  
 * <pre>
 * Copyright 2016 ResearchSpace

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 
</pre>
<p/>
*
*Wrapper for InfoOperations:
*<pre>
*GET http://$SERVER/api/info/settings/:DatasetPublishPopupCustomText
*and
*url -X PUT  -d "publish" https://demo.dataverse.org/api/admin/settings/:DatasetPublishPopupCustomText
*</pre>
*and
*
*/
public interface InfoOperations {
	
	DvMessage getDatasetPublishPopupCustomText () ;
	
	/**
	 * Deprecated, does not work for client calls from non-Localhost URLs from Dataverse 4.8 onwards 
    */
	DataverseResponse<Object> setDatasetPublishPopupCustomText (String text) ;

}
