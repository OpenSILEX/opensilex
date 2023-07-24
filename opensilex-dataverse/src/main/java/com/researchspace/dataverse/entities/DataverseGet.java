package com.researchspace.dataverse.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
public class DataverseGet {
	private String id;
	private String alias;
	private String name;
	private String affiliation;
	private String permissionRoot;
	private String description;
	private String ownerId;
	private Date creationDate;

	/**
	 * This can be a list of Strings or objects [displayOrder, contactEmail].
	 * Parsing as Object means that deserialisation won't fail
	 */
	private List<Object> dataverseContacts = new ArrayList<>();

	/**
	 * Gets list of contact emails independent of underlying representation
	 * @return
	 */
	@JsonIgnore
	public List<String> getContactEmails(){
		List<String> rc = new ArrayList<>();
		for (Object o: dataverseContacts) {
			if (o instanceof  String) {
				rc.add((String) o);
			} else {
				Map<String, String> object = (Map)o;
				rc.add(object.getOrDefault("contactEmail", ""));
			}
		}
		return rc;
	}

}
