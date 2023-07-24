/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

import lombok.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * /** <pre>
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
 * Simple POJO to set info for Dataset. 
 * @author rspace
 *
 */
@Data
@Builder
@AllArgsConstructor
public class DatasetFacade   {
	
	private @NonNull String title; 
	private @NonNull @Singular List<DatasetAuthor> authors;
	private @NonNull @Singular List<DatasetContact> contacts;
	private @NonNull String subject;
	private @NonNull @Singular List<DatasetDescription> descriptions;
	private String  depositor, subtitle, alternativeTitle;
	private URL alternativeURL;
	private @Singular List<DatasetKeyword> keywords;
	private @Singular List<DatasetTopicClassification> topicClassifications;
	private @Singular List<DatasetPublication> publications;
	private @Singular List<DatasetProducer> producers;
	private String note;
	private List<String> languages = new ArrayList<>();
	private Date productionDate;
	private String productionPlace;
	private @Singular List<DatasetContributor> contributors;
	
	/**
	 * Returns a copy if the internally stored Date
	 * @return
	 */
	public Date getProductionDate (){
		if(productionDate != null){
			return new Date(productionDate.getTime());
		} else {
			return null;
		}
		
	}
	/**
	 * Sets this object's date as a copy of the parameter Date.
	 * @param date
	 */
	public void setProductionDate(Date date) {
		this.productionDate = new Date (date.getTime());
	}
     /*
      * For testing
      */
	 DatasetFacade() {
		// TODO Auto-generated constructor stub
	}

}
