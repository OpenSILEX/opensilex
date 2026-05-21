<template>
  <opensilex-Card :label="t('AssociatedVariablesList.relatedVariables')" icon="ik#ik-layers">
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
              <div v-for="device in data.item.devices" :key="device.uri || device.name">
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

<script setup lang="ts">
import {inject, onMounted, ref} from 'vue';
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import { useI18n } from 'vue-i18n'
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import TableView from "@/components/common/views/TableView.vue";

//#region constant values & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const $store = useStore();
const { t } = useI18n();
const fields = [
  {
    key: "variableName",
    label: t("component.menu.variables"),
    sortable: true
  },
  {
    key: "devices",
    label: t("component.menu.devices"),
    sortable: false
  }
];
//#endregion

//#region: Component Refs
const tableRef = ref<InstanceType<typeof TableView> | null>(null);
//#endregion

//#region: Props and emits
interface Props {
  variableList: Array<NamedResourceDTOVariableModel>,
  deviceList: Array<DeviceGetDTO>,
  facilityUri: string;
}
const props = defineProps<Props>();
//#endregion

//# region: Reactive values (Data)
interface VariablesDevicesInformation{
  variableName: string,
  variableUri: string,
  devices: DeviceGetDTO[]
}
const variableAndDeviceListData = ref<VariablesDevicesInformation[]>([]);
//#endregion

//#region: Hooks
 onMounted(() => {
   loadVariableAndDeviceList();
 });
 //#endregion

//#region: Functions
/**
 * Creates the table data by placing devices with correct variables. Does this by checking if each device measures each variable.
 */
function loadVariableAndDeviceList() {

  variableAndDeviceListData.value = props.variableList.map(variable => {
    let devicesForVariable = props.deviceList.filter(device =>{
        let doesMeasureVariable: boolean = false;
        let relationsIndex = 0;
        const relations = device.relations || [];
        while(!doesMeasureVariable && relationsIndex < relations.length){
          const relation = relations[relationsIndex];
          if(isVariableRelation(relation)){
            doesMeasureVariable = relation.value == $opensilex.getShortUri(variable.uri);
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

function isVariableRelation(relation: RDFObjectRelationDTO): boolean {
  return $opensilex.compareUris($opensilex.Oeso.MEASURES_PROP_URI, relation.property)
}

/**
 * Custom filter for the table. Filters each line by the name of variable OR by the name of one of its devices.
 */
function customFilter(item, filter) {
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
//endregion
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