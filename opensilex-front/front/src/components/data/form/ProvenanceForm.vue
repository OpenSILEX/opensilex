<template>
  <b-form>
    <!-- URI-->
    <opensilex-UriForm
      :uri.sync="form.uri" 
      label="DocumentForm.uri"
      :editMode="editMode"
      :generated.sync="uriGenerated"
      helpMessage="DocumentForm.uri-help"
    ></opensilex-UriForm>
    
    <!-- Label -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="ProvenanceForm.name"
      helpMessage="ProvenanceForm.name-help"
      type="text"
      placeholder="ProvenanceForm.name-placeholder"
      :required="!disableValidation"
    ></opensilex-InputForm>

    <!-- description -->
    <opensilex-TextAreaForm            
      :value.sync="form.description"
      helpMessage="ProvenanceForm.description-help"
      label="ProvenanceForm.description"
      placeholder="ProvenanceForm.description-placeholder"
    ></opensilex-TextAreaForm>

    <!--activity -->
    <b-card :title="$t('ProvenanceForm.activity')" bg-variant="light">

      <!-- type -->
      <opensilex-TypeForm
          :type.sync="form.activity_type"
          :baseType="Prov.ACTIVITY_TYPE_URI"
          :required="!disableValidation"
          helpMessage="ProvenanceForm.type-help"
          placeholder="ProvenanceForm.type-placeholder"
      ></opensilex-TypeForm>

      <!-- start_date  & end_date-->
      <div class="row">
        <div class="col">
          <opensilex-DateTimeForm
            :value.sync="form.activity_start_date"
            label="ProvenanceForm.start"
            helpMessage="ProvenanceForm.start-help"
          ></opensilex-DateTimeForm>
        </div>

        <div class="col">
          <opensilex-DateTimeForm
            :value.sync="form.activity_end_date"
            label="ProvenanceForm.end"
            helpMessage="ProvenanceForm.end-help"
          ></opensilex-DateTimeForm>
        </div>
      </div>

    <!-- uri -->
    <opensilex-InputForm
      :value.sync="form.activity_uri"
      label="url"
      type="url"
      rules="url"
      helpMessage="ProvenanceForm.url-help"
    ></opensilex-InputForm>
    </b-card>

    <!-- agent -->
    <opensilex-ProvenanceAgentForm
      :values.sync="form.agents"
      :key="lang"
    ></opensilex-ProvenanceAgentForm>


  </b-form>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import Prov from "../../../ontologies/Prov";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ProvenanceForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $t: any;
  Prov = Prov;

  get lang() {
    return this.$store.getters.language;
  }

  @Ref("validatorRef") readonly validatorRef!: any;
  
  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        description: null,
        activity_type: null,
        activity_start_date: null,
        activity_end_date: null,
        activity_uri: null,
        agentTypes: [],
        agents: [{
            uris: [], 
            rdf_type: null
          }]
      };
    },
  })
  form: any;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Prop({
    default: false
  })
  disableValidation: boolean;



  typesToRemove = [];

  getEmptyForm() {
    return {
      uri: null,
      name: null,
      description: null,
      experiments: [],
      activity_type: null,
      activity_start_date: null,
      activity_end_date: null,
      activity_uri: null,
      agentTypes: [],
      agents: [{
            uris: [], 
            rdf_type: null
          }]
    };
  }  

  create(form) {
    let agents = []
    for (let i = 0; i < form.agents.length; i++) {
      for (let j = 0; j < form.agents[i].uris.length; j++) {
        agents.push({
          uri: form.agents[i].uris[j],
          rdf_type: form.agents[i].rdf_type
        })
      }
    }

    // format form
    let provenance = {
      uri: null,
      name: null,
      description: null,
      prov_activity: [],
      prov_agent: []
  }
    provenance.uri = this.form.uri;
    provenance.name = this.form.name;
    provenance.description = this.form.description;

    let activity = {
      rdf_type: this.form.activity_type,
      start_date: this.form.activity_start_date,
      end_date: this.form.activity_end_date,
      uri: this.form.activity_uri
    }
    provenance.prov_activity.push(activity);
    provenance.prov_agent = agents;

    return this.$opensilex
      .getService("opensilex.DataService")
      .createProvenance(provenance)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("provenance created", uri);
        provenance.uri = uri;
        return provenance;
      });
  }

  loadProvenance(selectedValue) {
    if (selectedValue != null) {
      this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(selectedValue.id)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          this.form.provenance = http.response.result;
        });
    }
  }

  update(form) {
    let agents = []
    for (let i = 0; i < form.agents.length; i++) {
      for (let j = 0; j < form.agents[i].uris.length; j++) {
        agents.push({
          uri: form.agents[i].uris[j],
          rdf_type: form.agents[i].rdf_type
        })
      }
    }

    // format form
    let provenance = {
      uri: null,
      name: null,
      description: null,
      prov_activity: [],
      prov_agent: []
  }
    provenance.uri = form.uri;
    provenance.name = form.name;
    provenance.description = form.description;

    let activity = {
      rdf_type: this.form.activity_type,
      start_date: this.form.activity_start_date,
      end_date: this.form.activity_end_date,
      uri: this.form.activity_uri
    }
    provenance.prov_activity.push(activity);
    provenance.prov_agent = agents;

    return this.$opensilex
      .getService("opensilex.DataService")
      .updateProvenance(provenance)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("provenance updated", uri);
        provenance.uri = uri;
        return provenance;
      });
  }


}
</script>
<style scoped lang="scss">
::v-deep .jsoneditor-menu {
  background-color: #a7a7a7;
  border-bottom: 1px solid #a7a7a7;
}
</style>

<i18n>
en:
  ProvenanceForm:
    name: Name
    name-help: Enter a name
    name-placeholder: Enter a name
    description: Description
    description-help: Describe provenance
    description-placeholder: Describe provenance
    agent: Agent
    agent-help: Select agents
    agent-placeholder: Select agents
    activity: Activity
    start: Start Date
    start-help: Start Date
    end: End Date
    end-date: End Date
    agents: Provenance agents
    add-agent: Add an agent
    type-placeholder: Select a type of activity
    type-help: Select a type of activity
    url-help: External link describing the activity

fr:
  ProvenanceForm:
    name: Nom
    name-help: Entrer un nom
    name-placeholder: Entrer un nom
    description: Description
    description-help: Décrire la provenance
    description-placeholder: Décrire la provenance
    agent: Agent
    agent-help: Selectionner agents
    agent-placeholder: Selectionner agents
    activity: Activité
    start: Date de début
    start-help: Date de début
    end: Date de fin
    end-date: Date de fin
    agents: Agents de la provenance
    add-agent: Ajouter un agent
    type-placeholder: Selectionner un type d'activité
    type-help: Selectionner un type d'activité
    url-help: Lien externe décrivant l'activity
</i18n>
