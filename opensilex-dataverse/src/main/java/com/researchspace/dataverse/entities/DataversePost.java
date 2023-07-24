package com.researchspace.dataverse.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;
/** <pre>
Copyright 2016 ResearchSpace

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
*/
@Data
@JsonFormat
public class DataversePost {
	private String id;
	private String alias;
	private String name;
	private String affiliation;
	private String permissionRoot;
	private String description;
	private String ownerId;
	private Date creationDate;
	
	
	private List<DataverseContacts> dataverseContacts;

}
