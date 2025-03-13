<template>
  <opensilex-Card label="AssociatedVariablesList.relatedVariables" icon="ik#ik-layers">
    <template v-slot:body>
      <opensilex-StringFilter
        :filter.sync="filter"
        @update="updateFilters()"
        :debounce="300"
        :lazy="false"
        placeholder="AssociatedVariablesList.variableNameFilter"
        ></opensilex-StringFilter>
      <opensilex-TableAsyncView
          ref="tableRef"
          :searchMethod="loadVariables"
          :fields="fields"
          defaulStortBy="name"
          :defaultPageSize="5"
      >
        <template v-slot:cell(name)="{data}">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{path:'/variables/details/'+encodeURIComponent(data.item.uri)}"
            ></opensilex-UriLink>
        </template>
      </opensilex-TableAsyncView>

    </template>
  </opensilex-Card>

</template>

<script lang="ts">
import {Component, Prop,PropSync,Ref} from "vue-property-decorator";
import Vue from 'vue';

import {VariablesService} from "opensilex-core/api/variables.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

@Component({})
export default class AssociatedVariableList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  @Ref("tableRef")readonly tableRef!:any;
  $service: OrganizationsService;

  @Prop()
  facilityUri;

  @PropSync("nameFilter")
  filter

  fields = [
    {
      key: "name",
      label:"component.common.name"
    }
  ];

  variables = [];
  variablesByUri: Map<String, VariableGetDTO> = new Map<String, VariableGetDTO>();

  loadVariables(){
    return this.$service.getFacilityVariables(
        this.facilityUri,
        undefined,
        undefined
    ).then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      console.log("http : ", http)
      return http;
    }).catch(this.$opensilex.errorHandler);
  // service
  //     .getFacilityVariables()
  //     .then((http: HttpResponse<OpenSilexResponse<Array<VariableGetDTO>>>) =>{
  //           this.variables = [];
  //           for(let i = 0; i < http.response.result.length; i++) {
  //             this.variablesByUri.set(
  //                 http.response.result[i].uri,
  //                 http.response.result[i]
  //             );
  //             this.variables.push({
  //               id: http.response.result[i].uri,
  //               label: http.response.result[i].name,
  //             });
  //           }
  //
  //           // force refresh of the table
  //       this.tableRef.refresh();
  //     })
  //     .catch(this.$opensilex.errorHandler);
   }

  // getVariablesName(uri : String): String {
  //   if(this.variablesByUri.has(uri)){
  //     return this.variablesByUri.get(uri).name;
  //   }
  //   return "";
  // }

  updateFilters(){
    this.tableRef.refresh();
  }

  created(){
    this.$service = this.$opensilex.getService("opensilex.OrganizationsService");
    this.loadVariables();
  }

}
</script>

<style scoped lang="scss">
.variables-list {
  text-overflow: ellipsis;
  overflow:hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 150px;
}
</style>

<i18n>
en:
  AssociatedVariablesList:
    variablesNameFilter: Search on variables name
    relatedVariables: Related Variables

fr:
  AssociatedVariablesList:
    variablesNameFilter: Chercher sur le nom de variable
    relatedVariables: variables connexes

</i18n>