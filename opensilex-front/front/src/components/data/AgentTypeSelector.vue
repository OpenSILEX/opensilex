<template>
  <opensilex-SelectForm
    label="ProvenanceView.agent_type"
    :selected.sync="agentTypeURI"
    :options="internalOptions"
    :multiple="multiple"
    :required="required"
    placeholder="ProvenanceView.agent_type-placeholder"
    @clear="$emit('clear')"
    @select="$emit('select')"
    @deselect="$emit('deselect')"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Prop, PropSync, Component } from "vue-property-decorator";
import Vue from "vue";
import ProvenanceAgentForm from "./form/ProvenanceAgentForm.vue";

@Component
export default class AgentTypeSelector extends Vue {

  $opensilex: any;

  @PropSync("selected")
  agentTypeURI;

  @Prop({default: false})
  multiple;

  @Prop({default: false})
  required;

  internalOptions: Array<any> = [];

  @Prop()
  exclusions: Set<string>;

  @Prop()
  options: Array<any>;

  mounted(){
    if(! this.options){
      this.internalOptions = ProvenanceAgentForm.getAgentTypes(this.$opensilex);
      return;
    }
    this.internalOptions = this.options.filter(option =>
        option.id == this.agentTypeURI || ! this.exclusions.has(option.id)
    );
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
