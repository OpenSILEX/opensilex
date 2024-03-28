<template>
  <div v-if="withBasicProperties">
    <div v-if="area.uri" class="container-fluid">
      <opensilex-PageHeader
          :title="area.name"
          description="component.area.title"
          icon="fa#sun"
          class="detail-element-header"
      ></opensilex-PageHeader>

      <opensilex-PageActions :returnButton="false" :tabs="true">
        <template v-slot>
          <b-nav-item
              :active="isDetailsTab()"
              :to="{ path: '/area/details/' + encodeURIComponent($route.params.uri) }"
          >{{ $t("component.common.details-label") }}
          </b-nav-item>

          <b-nav-item
              :active="isDocumentTab()"
              :to="{ path: '/area/documents/' + encodeURIComponent($route.params.uri) }"
          >{{ $t("component.project.documents") }}
          </b-nav-item>

          <b-nav-item
              :active="isAnnotationTab()"
              :to="{ path: '/area/annotations/' + encodeURIComponent($route.params.uri) }"
          >{{ $t("Annotation.list-title") }}
          </b-nav-item>
        </template>
      </opensilex-PageActions>

      <opensilex-PageContent>
        <b-row v-if="isDetailsTab()">
          <b-col sm="6">
            <opensilex-Card label="component.common.description">
              <template v-slot:rightHeader>
                <div class="ml-3">
                  <opensilex-EditButton
                      v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
                      :small="true"
                      label="Area.update"
                      @click="editArea(decodeURIComponent($route.params.uri))"
                  ></opensilex-EditButton>

                  <opensilex-DeleteButton
                      v-if="user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)"
                      label="Area.delete"
                      @click="deleteArea(decodeURIComponent($route.params.uri))"
                  ></opensilex-DeleteButton>
                </div>
              </template>
              <template v-slot:body>
                <opensilex-StringView
                    :value="area.uri"
                    label="component.area.details.uri"
                ></opensilex-StringView>
                <opensilex-StringView
                    :value="area.name"
                    label="component.area.details.name"
                ></opensilex-StringView>
                <opensilex-StringView
                    :value="nameType()"
                    label="component.area.details.rdfType"
                ></opensilex-StringView>
                <opensilex-TextView
                    v-if="area.description"
                    :value="area.description"
                    label="component.area.details.description"
                ></opensilex-TextView>
                <opensilex-GeometryCopy
                    :value="area.geometry"
                ></opensilex-GeometryCopy>
                <opensilex-MetadataView
                  v-if="area.publisher && area.publisher.uri"
                  :publisher="area.publisher"
                  :publicationDate="area.publication_date"
                  :lastUpdatedDate="area.last_updated_date"
              ></opensilex-MetadataView>
              </template>
            </opensilex-Card>
          </b-col>
        </b-row>

        <opensilex-DocumentTabList
            v-else-if="isDocumentTab()"
            ref="documentList"
            :modificationCredentialId="credentials.CREDENTIAL_AREA_MODIFICATION_ID"
            :uri="area.uri"
        ></opensilex-DocumentTabList>

        <opensilex-AnnotationList
            v-else-if="isAnnotationTab()"
            ref="annotationList"
            :deleteCredentialId="credentials.CREDENTIAL_AREA_DELETE_ID"
            :displayTargetColumn="false"
            :enableActions="true"
            :modificationCredentialId="credentials.CREDENTIAL_AREA_MODIFICATION_ID"
            :target="area.uri"
        ></opensilex-AnnotationList>
      </opensilex-PageContent>

      <opensilex-ModalForm
          v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
          ref="areaForm"
          :successMessage="successMessage"
          component="opensilex-AreaForm"
          createTitle="component.area.add"
          editTitle="component.area.update"
          icon="fa#sun"
          modalSize="lg"
          @onUpdate="loadArea(decodeURIComponent($route.params.uri))"
      ></opensilex-ModalForm>
    </div>
  </div>
  <div v-else class="detail-pop-up">
    <!-- Name -->
    <template v-if="showName">
      {{ area.uri === uri ? "" : loadArea(uri) }}
      <opensilex-StringView
          label="component.common.name"
      ></opensilex-StringView>
      <opensilex-UriLink
          class="personOrcid"
          :to="{ path: '/area/details/' + encodeURIComponent(area.uri), query: { experiment: encodeURIComponent(experiment)} }"
          :uri="area.uri"
          :value="area.name + ' (' + nameType().bold() + ')'"
          target="_blank"
      ></opensilex-UriLink>
    </template>
    <opensilex-StringView
        :value="authorName"
        label="component.area.details.publisher"
    ></opensilex-StringView>
    <div v-if="isViewAllInformation || !showName ">
      <opensilex-StringView
          v-if="area.description"
          :value="area.description"
          label="component.area.details.description"
      ></opensilex-StringView>
      <opensilex-GeometryCopy :value="area.geometry"></opensilex-GeometryCopy>
    </div>
    <p>
      <a id="show" v-if="showName" v-on:click="isViewAllInformation = !isViewAllInformation">{{
          $t(
              isViewAllInformation
                  ? "ScientificObjectDetailMap.seeMoreInformation"
                  : "ScientificObjectDetailMap.viewAllInformation"
          )
        }}</a>
    </p>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {AreaGetDTO} from 'opensilex-core/index';
import {UserGetDTO} from 'opensilex-security/index';
import {AreaService} from "opensilex-core/api/area.service";
import {SecurityService} from "opensilex-security/api/security.service";
import {OntologyService} from "opensilex-core/api/ontology.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import Oeso from "../../ontologies/Oeso";

@Component
export default class AreaDetails extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $t: any;
  $i18n: any;
  $route: any;
  areaService: AreaService;
  securityService: SecurityService;
  ontologyService: OntologyService;

  @Prop({
    default: true,
  })
  withBasicProperties;

  @Prop()
  uri;

  @Prop()
  experiment;

  @Prop()
  showName;

  @Ref("areaForm") readonly areaForm!: any;

  area: AreaGetDTO = {
    uri: null,
    name: null,
    publisher: null,
    rdf_type: null,
    description: null,
    geometry: null,
    event: null
  };

  isViewAllInformation: boolean = false;
  rdf_type: string;
  authorName: string = "";
  private lang: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.areaService = this.$opensilex.getService("opensilex.AreaService");
    this.securityService = this.$opensilex.getService("opensilex.SecurityService");
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService")
    if (this.withBasicProperties)
      this.loadArea(decodeURIComponent(this.$route.params.uri));
    else
      this.loadArea(this.uri);

  }

  loadArea(uri) {
    this.areaService
        .getByURI(uri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          this.area = http.response.result;
          if(this.area.event !== null){
            this.area.rdf_type = this.area.event.rdf_type;
          }
          this.rdf_type = this.area.rdf_type;
          this.loadAuthor(this.area.publisher);
        })
        .catch(this.$opensilex.errorHandler);
  }

  loadAuthor(publisher: UserGetDTO) {
    if(publisher) {
      this.authorName = publisher.first_name && publisher.last_name ?
      publisher.first_name + " " + publisher.last_name : publisher.uri;
    }
  }

  nameType() {
    if (this.user.locale != this.lang || this.rdf_type == this.area.rdf_type) {
      this.lang = this.user.locale;
      this.rdfTypeLabel();
    }
    return this.area.rdf_type;
  }

  successMessage(area) {
    return this.$i18n.t("component.area.label") + " " + area.name;
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/area/details/");
  }

  isDocumentTab() {
    return this.$route.path.startsWith("/area/documents/");
  }

  isAnnotationTab() {
    return this.$route.path.startsWith("/area/annotations/");
  }

  private rdfTypeLabel() {
    if (this.rdf_type) {
      this.$opensilex
          .getService<OntologyService>("opensilex.OntologyService")
          .getURILabel(this.rdf_type)
          .then((http: HttpResponse<OpenSilexResponse<string>>) => {
            this.area.rdf_type = http.response.result;
          })
          .catch((http: HttpResponse<OpenSilexResponse<string>>) => {
            if (http.status === 404) {
              this.area.rdf_type = this.rdf_type;
            } else {
              this.$opensilex.errorHandler(http);
            }
          });
    } else {
      this.area.rdf_type = "";
    }
  }

  editArea(uri) {
    this.areaService
        .getByURI(uri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          let form: any = http.response.result;

          if(form.rdf_type === Oeso.getShortURI(Oeso.TEMPORAL_AREA_TYPE_URI)){
            form.is_structural_area = false;
            form.rdf_type = this.$opensilex.getShortUri(form.event.rdf_type);
            form.start = form.event.start;
            form.end = form.event.end;
            form.is_instant = form.event.is_instant;
            form.event=null;
          }
          else {
            form.is_structural_area = true;
          }
          this.areaForm.showEditForm(form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  deleteArea(uri) {
    this.areaService
        .deleteArea(uri)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let message =
              this.$i18n.t("component.area.title") +
              " " +
              http.response.result +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
          this.$opensilex.showSuccessToast(message);
          this.$router.push({ path: "/experiment/map/" + this.$route.query.experiment });
        })
        .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style lang="scss" scoped>

.detail-pop-up{
  padding-top: 2%;
  margin-left: 0.5rem;
  margin-right: 0.5rem;
}

#show {
  color: #007bff;
  cursor: pointer;
}
</style>
<i18n>
en:
  component:
    area:
      title: Area
      add: Description of the area
      update: Update Area
      delete: Delete area
      details:
        uri: URI
        name: Name
        publisher: Publisher
        rdfType: Type
        description: Description
        geometry: Geometry
fr:
  component:
    area:
      title: Zone
      add: Description de la zone
      update: Mise à jour de la zone
      delete: Supprimer la zone
      details:
        uri: URI
        name: Nom
        publisher: Publieur
        rdfType: Type
        description: Description
        geometry: Géométrie
</i18n>