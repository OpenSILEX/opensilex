<template>
  <b-card :title="$t('ProvenanceForm.agents')" bg-variant="light">

    <b-button v-if="! agents || agents.length < initialAgentTypes.length"
              class="greenThemeColor"
              @click="addAgent">
      {{ $t('ProvenanceForm.add-agent') }}
    </b-button>

    <div class="row" v-for="agent in agents" v-bind:key="agent.rdf_type">

      <div class="col">
        <!-- agent type -->
        <opensilex-AgentTypeSelector
            :selected.sync="agent.rdf_type"
            :multiple="false"
            :options="initialAgentTypes"
            :exclusions="getUsedAgentTypes"
            @update:selected="onSelectedAgent(agent)"
            :key="agentSelectorRefreshKey"
        ></opensilex-AgentTypeSelector>
      </div>
      <div class="col">

        <!-- agent -->
        <opensilex-UserSelector
            v-if="agent.rdf_type === 'vocabulary:Operator'"
            :users.sync="agent.uris"
            label="ProvenanceForm.agent"
            helpMessage="ProvenanceForm.agent-help"
            :multiple="true"
        ></opensilex-UserSelector>

        <opensilex-DeviceSelector
            v-else-if="agent.rdf_type"
            label="ProvenanceForm.agent"
            :value.sync="agent.uris"
            :multiple="true"
            :type="agent.rdf_type"
            helpMessage="ProvenanceForm.agent-help"
        ></opensilex-DeviceSelector>
      </div>
    </div>
  </b-card>
</template>

<script lang="ts">
import Vue from "vue";
import {PropSync, Component } from "vue-property-decorator";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import Oeso from "../../../ontologies/Oeso";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import { RDFTypeDTO } from 'opensilex-core/index';

@Component
export default class ProvenanceAgentForm extends Vue {

  $opensilex: any;

  @PropSync("values")
  agents: Array<any>;

  initialAgentTypes: Array<any> = [];

  agentSelectorRefreshKey = 0;

  created() {
    this.initialAgentTypes = ProvenanceAgentForm.getAgentTypes(this.$opensilex);
  }

  private _getUsedAgentTypes;

  get getUsedAgentTypes(): Set<string> {
    if(! this.initialAgentTypes || this.initialAgentTypes.length == 0){
      return new Set();
    }
    // compute the Set of already used agent types
    return new Set(
        this.agents
            .filter(agent => agent && agent.rdf_type)
            .map(agent => agent.rdf_type)
    );
  }

  set getUsedAgentTypes(value) {
    this._getUsedAgentTypes = value;
  }


  addAgent() {
    this.agents.push({uris: [], rdf_type: null});
  }

  static getAgentTypes(opensilex: OpenSilexVuePlugin): Array<any> {

    let agentTypes: Array<any> = [];
    let service: OntologyService = opensilex.getService("opensilex.OntologyService");

    service.getRDFType(Oeso.OPERATOR_TYPE_URI, undefined)
        .then((http: HttpResponse<OpenSilexResponse<RDFTypeDTO>>) => {
          agentTypes.push({
            id: http.response.result.uri,
            label: http.response.result.name,
          });
        })
        .catch(opensilex.errorHandler);

    service.getSubClassesOf(Oeso.DEVICE_TYPE_URI, true)
        .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            agentTypes.push({
              id: http.response.result[i].uri,
              label: http.response.result[i].name,
            });
          }
        })
        .catch(opensilex.errorHandler);

    return agentTypes;
  }

  onSelectedAgent(agent){
    agent.uris=[];

    // Update selector refresh key to enforce AgentTypeSelector re-render
    // Ensure that each AgentTypeSelector has a consistent exclusions set, according other AgentTypeSelector selected type(s)
    this.agentSelectorRefreshKey++
  }


}
</script>

<style scoped>

</style>