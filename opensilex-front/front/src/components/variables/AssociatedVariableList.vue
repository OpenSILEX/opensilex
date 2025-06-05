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
            <template v-for="(name, index) in data.item.devices.split(', ')">
              <opensilex-UriLink
                  :key="index"
                  :uri="data.item.uri[index]"
                  :value="name"
                  :to="{ path: '/device/details/' + encodeURIComponent(data.item.uri[index]) }"
              />
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
import {NamedResourceDTODeviceModel} from "opensilex-core/model/namedResourceDTODeviceModel";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {DevicesService, DeviceGetDetailsDTO} from "opensilex-core/index";

@Component({})
export default class AssociatedVariablesList extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;
  service: DevicesService;
  @Ref("tableRef") readonly tableRef!: any;

  @Prop()
  variableList!: Array<NamedResourceDTOVariableModel>;

  @Prop()
  deviceList!: Array<NamedResourceDTODeviceModel>;

  variableAndDeviceListData: Array<any> = [];

  fields = [
    {
      key: "variableName",
      label: "component.common.name"
    },
    {
      key: "devices",
      label: "AssociatedVariablesList.devices"
    }
  ];

  created() {
    this.service = this.$opensilex.getService("opensilex.DevicesService");
  }

  async mounted() {
    await this.loadVariableAndDeviceList();
  }

  async loadVariableAndDeviceList() {
    const promises = this.variableList.map(variable => {
      return this.service.searchDevices(
          undefined,
          true,
          undefined,
          variable.uri,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined
      ).then((http: HttpResponse<OpenSilexResponse<Array<DeviceGetDetailsDTO>>>) => {
        const devices = http.response.result;
        return {
          variableName: variable.name,
          variableUri: variable.uri,
          devices: devices.map(d => d.name).join(", "),
          uri: devices.map(d => d.uri)
        };
      }).catch((error: any) => {
        this.$opensilex.errorHandler(error);
        return {
          variableName: variable.name,
          variableUri: variable.uri,
          devices: "Erreur de chargement",
          uri: []
        };
      });
    });

    this.variableAndDeviceListData = await Promise.all(promises);
  }
}
</script>


<style scoped lang="scss">
.variables-list {
  //text-overflow: ellipsis;
  overflow:hidden;
  white-space: nowrap;
  display: inline-block;
  //max-width: 150px;
  max-width:100%;
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