<template>
  <opensilex-Card label="AssociatedVariablesList.relatedVariables" icon="ik#ik-layers">
    <template v-slot:body>
      <opensilex-TableView
          ref="tableRef"
          :globalFilterField="true"
          :items="variableAndDeviceList"
          :fields="fields"
          defaulStortBy="name"
          :defaultPageSize="5"
      >
        <template v-slot:cell(variableName)="{data}">
          <opensilex-UriLink
            :uri="data.item.variableUri"
            :value="data.item.variableName"
            :to="{path:'/variable/details/'+encodeURIComponent(data.item.uri)}"
            ></opensilex-UriLink>
        </template>

        <template v-slot:cell(devices)="{data}">
          <opensilex-UriLink
              :uri="data.item.uri"
              :value="data.item.devices"
              :to="{path:'/device/details/'+encodeURIComponent(data.item.uri)}"
          ></opensilex-UriLink>

        </template>
      </opensilex-TableView>

    </template>
  </opensilex-Card>

</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from 'vue';
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";


@Component({})
export default class AssociatedVariablesList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  @Ref("tableRef")readonly tableRef!:any;

  @Prop()
  variableList: Array<NamedResourceDTOVariableModel>;
  @Prop()
  deviceList : Array<DeviceGetDTO>;

  get variableAndDeviceList(): Array<{variableName: string,variableUri: string, devices: string[]}>{
    var list = []
    this.variableList.forEach( variable => {
      let res = {
        variableName: variable.name,
        variableUri:variable.uri,
        devices: this.deviceList.map(device => device.name)
      }
      list.push(res);
    })
    return list;
  }

  fields = [
    {
      key: "variableName",
      label:"component.common.name"
    },
    {
      key: "devices",
      label: "AssociatedVariablesList.devices"
    }
  ];
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
    devices: devices

fr:
  AssociatedVariablesList:
    variablesNameFilter: Chercher sur le nom de variable
    relatedVariables: variables connexes
    devices: dispositifs
</i18n>