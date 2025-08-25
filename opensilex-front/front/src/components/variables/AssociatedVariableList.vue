<template>
  <opensilex-Card label="AssociatedVariablesList.relatedVariables" icon="ik#ik-layers">
    <template v-slot:body>
      <opensilex-TableView
          ref="tableRef"
          :globalFilterField="true"
          :items="variableAndDeviceListData"
          :fields="fields"
          sortBy="variableName"
          :defaultPageSize="5"
          :customFilter="customFilter"
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
import VueI18n from "vue-i18n";
import {OpenSilexStore} from "../../models/Store"
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component({})
export default class AssociatedVariablesList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $i18n: VueI18n;
  $store: OpenSilexStore;
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
      label: "component.menu.variables",
      sortable: true
    },
    {
      key: "devices",
      label: "component.menu.devices",
      sortable: false
    }
  ];

  mounted(){
    this.loadVariableAndDeviceList();
  }

  /**
   * Creates the table data by placing devices with correct variables. Does this by checking if each device measures each variable.
   */
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
    return this.$opensilex.checkURIs(this.$opensilex.Oeso.MEASURES_PROP_URI, relation.property)
  }

  /**
   * Custom filter for the table. Filters each line by the name of variable OR by the name of one of its devices.
   */
  customFilter(item, filter) {
    try {
      const regex = new RegExp(filter, "i");

      // Safely join all device names into one string
      const deviceString = (item.devices || [])
          .map(device => device.name || "")
          .join(" ");


      return regex.test(item.variableName) || regex.test(deviceString);
    } catch (e) {
      // Fallback to simple substring match
      const deviceString = (item.devices || [])
          .map(device => device.name || "")
          .join(" ")
          .toLowerCase();

      return (
          (item.variableName || "").toLowerCase().includes(filter.toLowerCase()) ||
          deviceString.includes(filter.toLowerCase())
      );
    }
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
    relatedVariables: Related Variables and Devices

fr:
  AssociatedVariablesList:
    variablesNameFilter: Rechercher par nom de variable
    relatedVariables: Variables et appareils liés
</i18n>