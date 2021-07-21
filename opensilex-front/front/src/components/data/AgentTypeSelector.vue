<template>
  <opensilex-SelectForm
    label="ProvenanceView.agent_type"
    :selected.sync="agentTypeURI"
    :options="agentTypes"
    :multiple="multiple"
    :required="required"
    placeholder="ProvenanceView.agent_type-placeholder"
    @clear="$emit('clear')"
    @select="$emit('select')"
    @deselect="$emit('deselect')"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Prop, PropSync, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { RDFTypeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class AgentTypeSelector extends Vue {
  $opensilex: any;
  $store: any;
  service: any;

  @PropSync("agentType")
  agentTypeURI;

  @Prop({default: false})
  multiple;

  @Prop({default: false})
  required;

  @Prop({default: (() => [])})
  typesToRemove;

  agentTypes: any[] = [];

  mounted() {
    this.loadAgentTypes();
  }

  loadAgentTypes() {

    this.$opensilex.getService("opensilex.OntologyService")
    .getRDFType([Oeso.OPERATOR_TYPE_URI], undefined)
    .then((http: HttpResponse<OpenSilexResponse<RDFTypeDTO>>) => { 
        if (!this.typesToRemove.includes(http.response.result.uri)) {
          this.agentTypes.push({
            id: http.response.result.uri,
            label: http.response.result.name,
          });
        } 
        
      
    })
    .catch(this.$opensilex.errorHandler);

    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf(Oeso.DEVICE_TYPE_URI, true)
    .then((http: HttpResponse<OpenSilexResponse<Array<RDFTypeDTO>>>) => {
      for (let i = 0; i < http.response.result.length; i++) { 
        if (!this.typesToRemove.includes(http.response.result[i].uri)) {  
          this.agentTypes.push({
            id: http.response.result[i].uri,
            label: http.response.result[i].name,
          });
        }
      }
    })
    .catch(this.$opensilex.errorHandler);   

  }

}
</script>


<style scoped lang="scss">
</style>

<i18n>

en:
  ProvenanceView:
    add: Add provenance
    description: Describe data provenance
    name: Name
    activity_type: Activity type
    agent_type: Agent type
    agent: Agent
    activity_start_date: Start date
    activity_end_date: End date
    name-placeholder: Enter provenance name
    activity_type-placeholder: Select a type of activity
    agent_type-placeholder: Select a type of agent

fr:
  ProvenanceView:
    add: Ajouter une provenance
    description: Décrire la provenance de données
    name: Nom
    activity_type: Type d'activité
    agent_type: Type d'agent
    agent: Agent
    activity_start_date: Date de début
    activity_end_date: Date de fin
    name-placeholder: Entrer un nom de provenance
    activity_type-placeholder: Selectionner un type d'activité
    agent_type-placeholder: Selectionner un type d'agent
  
</i18n>
