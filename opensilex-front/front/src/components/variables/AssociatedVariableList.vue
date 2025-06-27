<template>
  <opensilex-Card label="AssociatedVariablesList.relatedVariables" icon="ik#ik-layers">
    <template v-slot:body>
      <opensilex-TableView
          ref="tableRef"
          :globalFilterField="true"
          :items="variableAndDeviceListData"
          :fields="fields"
          :defaultStortBy="variableName"
          :defaultPageSize="5"
      >
        <template v-slot:cell(variableName)="{data}">
          <opensilex-UriLink
            :uri="data.item.variableUri"
            :value="data.item.variableName"
            :to="{path:'/variable/details/'+encodeURIComponent(data.item.variableUri)}"
            ></opensilex-UriLink>
        </template>

        <template v-slot:cell(devices)="{ data }">
          <div class="variables-list">
            <template v-if="data.item.devices && data.item.devices.length">
              <div v-for="(device, index) in data.item.devices" :key="index">
                <opensilex-UriLink
                  :uri="device.uri"
                  :value="device.name"
                  :to="{ path: '/device/details/' + encodeURIComponent(device.uri) }"
                />
              </div>
            </template>
          </div>
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
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";

@Component({})
export default class AssociatedVariablesList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  @Ref("tableRef") readonly tableRef!: any;

  @Prop()
  variableList!: Array<NamedResourceDTOVariableModel>;

  @Prop()
  deviceList!: Array<DeviceGetDTO>;

  @Prop()
  facilityUri!: string;

  variableAndDeviceListData: Array<any> = [];

  fields = [
    {
      key: "variableName",
      label: "AssociatedVariablesList.variables"
    },
    {
      key: "devices",
      label: "AssociatedVariablesList.devices"
    }
  ];

  mounted(){
    this.loadVariableAndDeviceList();
  }

  loadVariableAndDeviceList() {

    this.variableAndDeviceListData = this.variableList.map(variable => {
      let devicesForVariable = this.deviceList.filter(device =>{
          let doesMeasureVariable: boolean = false;
          let relationsIndex = 0;
          while(!doesMeasureVariable && relationsIndex < device.relations.length){
            const relation = device.relations[relationsIndex];
            if(this.isVariableRelation(relation)){
              doesMeasureVariable = relation.value == this.$opensilex.getShortUri(variable.uri);
            }
            relationsIndex++;
          }
          return doesMeasureVariable;
        }
      );
      return {
        variableName: variable.name,
        variableUri: variable.uri,
        devices: devicesForVariable
      };
    });
  }

  private isVariableRelation(relation: RDFObjectRelationDTO): boolean {
    const measures_prop = this.$opensilex.Oeso.MEASURES_PROP_URI;
    return relation.property == measures_prop || relation.property == this.$opensilex.Oeso.getShortURI(measures_prop);
  }
}
</script>


<style scoped lang="scss">
.variables-list {
  overflow:hidden;
  white-space: nowrap;
  display: inline-block;
  max-width:100%;
}
</style>

<i18n>
en:
  AssociatedVariablesList:
    variablesNameFilter: Search by variable name
    relatedVariables: Related Variables
    devices: Devices
    variables: Variables

fr:
  AssociatedVariablesList:
    variablesNameFilter: Rechercher par nom de variable
    relatedVariables: Variables liées
    devices: Appareils
    variables: Variables
</i18n>