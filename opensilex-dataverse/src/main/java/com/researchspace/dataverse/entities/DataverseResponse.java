/*
 * 
 */
package com.researchspace.dataverse.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
/**
 *  <pre>
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
 * Wrapper over response, can include <code>data</code> or an error<code>message</code> but not both
 * @author rspace
 *
 * @param <T> The datatype of the response
 */
@Data
public class DataverseResponse <T> {
	
	private String status;
	private T data;
	@JsonDeserialize(using = ObjectOrStringMessageDeserializer.class )
	private String message;

}
