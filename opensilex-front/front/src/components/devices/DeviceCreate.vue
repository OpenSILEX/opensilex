<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-thermometer"
      title="DeviceCreate.title"
      description="DeviceCreate.description"
    ></opensilex-PageHeader>
    <opensilex-PageActions :returnButton="true" >
    </opensilex-PageActions>
    <opensilex-PageContent>
      <opensilex-TypeForm
        :type.sync="selectedType"
        :baseType="$opensilex.Oeso.DEVICE_TYPE_URI"
        :placeholder="$t('DeviceCreate.form-type-placeholder')"
        @update:type="refreshTable"
      ></opensilex-TypeForm>
      <opensilex-DeviceTable v-if="selectedType" ref="deviceTable" :deviceType="selectedType" :key="tabulatorRefresh"></opensilex-DeviceTable>
    </opensilex-PageContent>
    <b-modal ref="helpModal" size="xl" hide-header ok-only>
      <opensilex-DeviceHelp></opensilex-DeviceHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Ref } from "vue-property-decorator";
// @ts-ignore
import { OntologyService, ResourceTreeDTO } from "opensilex-core/index";
import Oeso from "../../ontologies/Oeso";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class DeviceCreate extends Vue {
  service: OntologyService;
  $opensilex: any;
  $store: any;
  deviceTypes: any = [];
  selectedType: string = null;
  tabulatorRefresh = 0;

  @Ref("helpModal") readonly helpModal!: any;
  @Ref("deviceTable") readonly deviceTable!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.loadDeviceTypes();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadDeviceTypes();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  loadDeviceTypes() {
    this.deviceTypes = [];
    let ontoService: OntologyService = this.$opensilex.getService(
      "opensilex.OntologyService"
    );

    ontoService
      .getSubClassesOf(Oeso.DEVICE_TYPE_URI, true)
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        for (let i = 0; i < http.response.result.length; i++) {
            let resourceDTO = http.response.result[i];
            this.deviceTypes.push({
                value: resourceDTO.uri,
                text: resourceDTO.name,
            });
        }
      })
      .catch(this.$opensilex.errorHandler);
  }

  refreshTable() {
    let deviceTable: any = this.deviceTable;
    this.tabulatorRefresh++;
    deviceTable.updateColumns();
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DeviceCreate:
    title: Declare Device
    description: Add sensors, vectors, actuators ...
    backToList: Return to device list
    form-type-placeholder: Please select a type
fr:
  DeviceCreate:
    title: Déclarer un nouveau dispositif
    description: Créer des capteurs, des vecteurs, des actionneurs ...
    backToList: Revenir à la liste des appareils
    form-type-placeholder: Veuillez sélectionner un type d'appareil
  
</i18n>