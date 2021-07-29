<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.data.provenance"
      description="ProvenanceView.description"
    ></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton
          @click="createProvenance()"
          label="ProvenanceView.add"
        ></opensilex-CreateButton>
      </template>
    </opensilex-PageActions>

    <opensilex-ModalForm
      ref="provenanceForm"
      component="opensilex-ProvenanceForm"
      createTitle="ProvenanceView.add"
      editTitle="ProvenanceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onCreate="provList.refresh()"
      @onUpdate="provList.refresh()"
      :successMessage="successMessage"
    ></opensilex-ModalForm>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProvenanceList 
          ref="provList"
          @onEdit="editProvenance"
          @onDelete="deleteProvenance"
        ></opensilex-ProvenanceList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import { ProvenanceGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import Vue from "vue";

@Component
export default class ProvenanceView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  agentTypes: any[] = [];

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("provList") readonly provList!: any;
  @Ref("provenanceForm") readonly provenanceForm!: any;

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
  }

  createProvenance() {
    this.provenanceForm.showCreateForm();
  }

  editProvenance(uri: string) {
    this.service
      .getProvenance(uri)
      .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
        let updateForm = this.convertDtoBeforeEditForm(http.response.result);
        this.provenanceForm.showEditForm(updateForm);
      })
      .catch(this.$opensilex.errorHandler);
    
  }

  convertDtoBeforeEditForm(dto: ProvenanceGetDTO) {
    let form = {
      uri: dto.uri,
      name: dto.name,
      description: dto.description,
      activity_type: null,
      activity_start_date: null,
      activity_end_date: null,
      activity_uri: null,
      agents: [],
      items: [0]
    }

    if (dto.prov_activity != null && dto.prov_activity.length>0) {
      form.activity_type = dto.prov_activity[0].rdf_type;
      form.activity_start_date = dto.prov_activity[0].start_date;
      form.activity_end_date = dto.prov_activity[0].end_date;
      form.activity_uri = dto.prov_activity[0].uri;
    }

    let uniqueTypes = new Set<string>();
    if (dto.prov_agent != null) {
      dto.prov_agent.forEach(agent => {
            uniqueTypes.add(agent.rdf_type);
        });
    }        

    for (let type of uniqueTypes) {
      let agentsByType = [];
      for (let agent of dto.prov_agent) {
        if (agent.rdf_type == type) {
          agentsByType.push(agent.uri)
        }
      }
      form.agents.push({
        rdf_type: type,
        uris: agentsByType
      })
    }

    return form;
  }

  deleteProvenance(uri: string) {
    console.debug("deleteProvenance " + uri);
    this.service
      .deleteProvenance(uri)
      .then(() => {
        this.provList.refresh();
        let message =
          this.$i18n.t("ProvenanceView.title") +
          " " +
          uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
  }

  successMessage(form) {
    return this.$t("ProvenanceView.success-message") + " " + form.name;
  }

}
</script>


<style scoped lang="scss">
</style>

<i18n>

en:
  ProvenanceView:
    add: Add provenance
    update: Update provenance
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
    success-message: Provenance

fr:
  ProvenanceView:
    add: Ajouter une provenance
    update: Modifier une provenance
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
    success-message: La provenance
  
</i18n>