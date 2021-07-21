<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#seedling"
      :title="provenance.name"
      description="ProvenanceDetailsPage.title"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs=true :returnButton="true">
      <b-nav-item
      :active="isDetailsTab()"
      :to="{path: '/provenances/details/' + encodeURIComponent(uri)}"
      >{{ $t('component.common.details-label') }}
      </b-nav-item>

      <b-nav-item
      :active="isAnnotationTab()"
      :to="{ path: '/provenances/annotations/' + encodeURIComponent(uri) }"
      >{{ $t("Annotation.list-title") }}
      </b-nav-item>

      <b-nav-item
      :active="isDocumentTab()"
      :to="{path: '/provenances/documents/' + encodeURIComponent(uri)}"
      >{{ $t('component.project.documents') }}
      </b-nav-item>

    </opensilex-PageActions>

    <opensilex-PageContent>
      <b-row v-if="isDetailsTab()">
        <b-col>
          <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
            <template v-slot:rightHeader>              
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_DATA_DELETE_ID)"
                @click="showEditForm"
              ></opensilex-EditButton>
              <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_DATA_DELETE_ID)"
                @click="deleteProvenance"
              ></opensilex-DeleteButton>
            </template>

            <template v-slot:body>
              <opensilex-UriView
                label="ProvenanceDetails.uri"
                :uri="provenance.uri"
              ></opensilex-UriView>
              <opensilex-StringView
                label="ProvenanceDetails.label"
                :value="provenance.name"
              ></opensilex-StringView>

              <opensilex-StringView 
                v-if="provenance.prov_activity != null && provenance.prov_activity.length>0"
                label="ProvenanceDetailsPage.activity"
                :value="provenance.prov_activity[0].name"
              ></opensilex-StringView>

              <opensilex-StringView
                v-if="provenance.prov_activity != null && provenance.prov_activity.length>0"
                label="ProvenanceDetailsPage.activity_start_date"
                :value="provenance.prov_activity[0].start_date"
              ></opensilex-StringView>

              <opensilex-StringView
                v-if="provenance.prov_activity != null && provenance.prov_activity.length>0"
                label="ProvenanceDetailsPage.activity_end_date"
                :value="provenance.prov_activity[0].end_date"
              ></opensilex-StringView>

              <opensilex-UriView
                v-if="provenance.prov_activity != null && provenance.prov_activity.length>0"
                title="ProvenanceDetailsPage.activity_external_link"
                :value="provenance.prov_activity[0].uri"
                :uri="provenance.prov_activity[0].uri"
              ></opensilex-UriView>

            </template>
          </opensilex-Card>
        </b-col>
        <b-col>
          <opensilex-Card label="ProvenanceDetailsPage.agents" icon="ik#ik-clipboard">
            <template v-slot:body>
              <opensilex-TableView
                ref="tableRef"
                :items="provenance.prov_agent"
                defaultSortBy="name"
                :defaultPageSize="10"
                :fields="tableFields"
              >
                <template v-slot:cell(name)="{data}">
                  <opensilex-UriLink v-if="data.item.rdf_type != 'vocabulary:Operator'"
                    :uri="data.item.uri"
                    :value="data.item.name"
                    :to="{path: '/device/details/'+ encodeURIComponent(data.item.uri)}"
                  ></opensilex-UriLink>
                  <opensilex-UriLink v-else
                    :uri="data.item.uri"
                    :value="data.item.name"
                  ></opensilex-UriLink>
                </template>
                
              </opensilex-TableView>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>

      <opensilex-DocumentTabList
        v-else-if="isDocumentTab()"
        ref="documentTabList"
        :uri="uri"        
        :modificationCredentialId="credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID"
      ></opensilex-DocumentTabList>

      <opensilex-AnnotationList
      v-else-if="isAnnotationTab()"
      ref="annotationList"
      :target="uri"
      :displayTargetColumn="false"
      :enableActions="true"
      :modificationCredentialId="credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_GERMPLASM_DELETE_ID"
      ></opensilex-AnnotationList>
      
    </opensilex-PageContent>   

    <opensilex-ModalForm
      ref="provenanceForm"
      component="opensilex-ProvenanceForm"
      createTitle="ProvenanceView.add"
      editTitle="ProvenanceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onUpdate="loadProvenance()"
      :successMessage="successMessage"
    ></opensilex-ModalForm>

  </div>  
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ProvenanceGetDTO, DataService, ProvenanceUpdateDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import DocumentTabList from "../documents/DocumentTabList.vue";
import { UserGetDTO } from "opensilex-security/model/userGetDTO";


@Component
export default class ProvenanceDetailsPage extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;
  service: DataService;

  uri: string = null;
  addInfo = [];
  experimentName: any = "";

  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("documentTabList") readonly documentTabList!: DocumentTabList;
  @Ref("provenanceForm") readonly provenanceForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  isDetailsTab() {
      return this.$route.path.startsWith("/provenances/details/");
  }

  isDocumentTab() {
      return this.$route.path.startsWith("/provenances/documents/");
  }

  isAnnotationTab() {
      return this.$route.path.startsWith("/provenances/annotations/");
  } 

  provenance: ProvenanceGetDTO = {
        uri: null,
        name: null,
        description: null,
        prov_activity: [],
        prov_agent: [],
      };

  refresh() {
    this.loadProvenance();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.refresh();
  }

  loadProvenance() {
    new Promise((resolve, reject) => { this.service
      .getProvenance(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
        let promiseArray = [];
        let prov = http.response.result;
        
        if (prov.prov_activity != null && prov.prov_activity.length>0) {
          let promiseActivity = this.$opensilex.getService("opensilex.OntologyService")
            .getURILabel(prov.prov_activity[0].rdf_type)
            .then((http: HttpResponse<OpenSilexResponse<String>>) => {
              prov.prov_activity[0]["name"] = http.response.result;
            })
            .catch(this.$opensilex.errorHandler);
          promiseArray.push(promiseActivity);
        }
        if (prov.prov_agent != null) {
          for (let i = 0; i < prov.prov_agent.length; i++) {
            let promiseAgent;
            if (prov.prov_agent[i].rdf_type == "vocabulary:Operator") {
              promiseAgent = this.$opensilex.getService("opensilex.SecurityService")
              .getUser(prov.prov_agent[i].uri)
              .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
                prov.prov_agent[i]["name"] = http.response.result.first_name + " " + http.response.result.last_name;
              })
              .catch(this.$opensilex.errorHandler);
            } else {
              promiseAgent = this.$opensilex.getService("opensilex.OntologyService")
              .getURILabel(prov.prov_agent[i].uri)
              .then((http: HttpResponse<OpenSilexResponse<String>>) => {
                prov.prov_agent[i]["name"] = http.response.result;
              })
              .catch(this.$opensilex.errorHandler);
            }
            promiseArray.push(promiseAgent);
          }
        }    
        Promise.all(promiseArray).then((values) => {
          resolve(http);
          this.provenance = prov;
        });    
      })
      .catch(reject);
    });
  }

  showEditForm() {
    let updateForm = this.convertDtoBeforeEditForm();
    this.provenanceForm.showEditForm(updateForm);
  }

  convertDtoBeforeEditForm() {    

    let form = {
      uri: this.provenance.uri,
      name: this.provenance.name,
      description: this.provenance.description,
      activity_type: null,
      activity_start_date: null,
      activity_end_date: null,
      activity_uri: null,
      agents: []
    }

    if (this.provenance.prov_activity != null && this.provenance.prov_activity.length>0) {
      form.activity_type = this.provenance.prov_activity[0].rdf_type;
      form.activity_start_date = this.provenance.prov_activity[0].start_date;
      form.activity_end_date = this.provenance.prov_activity[0].end_date;
      form.activity_uri = this.provenance.prov_activity[0].uri;
    }

    let uniqueTypes = new Set<string>();
    if (this.provenance.prov_agent != null) {
      this.provenance.prov_agent.forEach(agent => {
            uniqueTypes.add(agent.rdf_type);
        });
    }        

    for (let type of uniqueTypes) {
      let agentsByType = [];
      for (let agent of this.provenance.prov_agent) {
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

  deleteProvenance() {
    this.service
      .deleteProvenance(this.provenance.uri)
      .then(() => {
        let message =
          this.$i18n.t("ProvenanceView.title") +
          " " +
          this.provenance.uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$router.push({
            path: "/provenances"
          });
      })
      .catch(this.$opensilex.errorHandler);
  }

  tableFields = [
    {
      key: "name",
      label: "component.common.name",
      sortable: true,
    },
    {
      key: "rdf_type",
      label: "component.common.type",
    }
  ];
  
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ProvenanceDetailsPage:
    title: Provenance
    description: 
    agents: Provenance agents
    activity: Activity type
    activity_start_date: Start Date
    activity_end_date: End Date
    activity_external_link: External link
    agent-settings: Settings

fr:
  ProvenanceDetailsPage:
    title: Provenance
    agents: Agents de la provenance
    activity: Type d'activité
    activity_start_date: Date de début
    activity_end_date: Date de fin
    activity_external_link: Lien externe
    agent-settings: Paramètres
</i18n>
