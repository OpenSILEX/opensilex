<template>
  <div v-if="withBasicProperties">
    <div v-if="area.uri" class="container-fluid">
      <opensilex-PageHeader
          :title="area.name"
          description="component.area.title"
          icon="fa#sun"
          class="detail-element-header"
      ></opensilex-PageHeader>

      <opensilex-PageActions :returnButton="true" :tabs="true">
        <b-nav-item
            :active="isDetailsTab()"
            :to="{ path: '/area/details/' + encodeURIComponent(uri) }"
        >{{ $t("component.common.details-label") }}
        </b-nav-item>

  <!--      <b-nav-item
            :active="isDocumentTab()"
            :to="{ path: '/area/documents/' + encodeURIComponent(uri) }"
        >{{ $t("component.project.documents") }}
        </b-nav-item>

        <b-nav-item
            :active="isAnnotationTab()"
            :to="{ path: '/area/annotations/' + encodeURIComponent(uri) }"
        >{{ $t("Annotation.list-title") }}
        </b-nav-item>
  -->
      </opensilex-PageActions>

      <opensilex-PageContent>
        <b-row v-if="isDetailsTab()">
          <b-col sm="5">
            <opensilex-Card label="component.common.description">
              <template v-slot:rightHeader>
                <div class="ml-3">
                  <opensilex-EditButton
                      v-if="user.hasCredential(credentials.CREDENTIAL_AREA_MODIFICATION_ID)"
                      :small="true"
                      label="Area.update"
                      @click="editArea()"
                  ></opensilex-EditButton>

                  <opensilex-DeleteButton
                      v-if="user.hasCredential(credentials.CREDENTIAL_AREA_DELETE_ID)"
                      label="component.area.delete"
                      @click="deleteArea()"
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
                    :value="authorName"
                    label="component.area.details.author"
                ></opensilex-StringView>
                <opensilex-StringView
                    :value="nameType()"
                    label="component.area.details.rdfType"
                ></opensilex-StringView>
                <opensilex-StringView
                    v-if="area.description"
                    :value="area.description"
                    label="component.area.details.description"
                ></opensilex-StringView>
                <opensilex-GeometryCopy
                    :value="area.geometry"
                ></opensilex-GeometryCopy>
              </template>
            </opensilex-Card>
          </b-col>
        </b-row>

        <opensilex-DocumentTabList
            v-else-if="isDocumentTab()"
            ref="documentList"
            :modificationCredentialId="credentials.CREDENTIAL_AREA_MODIFICATION_ID"
            :uri="uri"
        ></opensilex-DocumentTabList>

        <opensilex-AnnotationList
            v-else-if="isAnnotationTab()"
            ref="annotationList"
            :deleteCredentialId="credentials.CREDENTIAL_AREA_DELETE_ID"
            :displayTargetColumn="false"
            :enableActions="true"
            :modificationCredentialId="credentials.CREDENTIAL_AREA_MODIFICATION_ID"
            :target="uri"
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
          @onUpdate="loadArea"
      ></opensilex-ModalForm>
    </div>
  </div>
  <div v-else>
    <!-- Name -->
    <template v-if="showName">
      {{ area.uri === uri ? "" : loadArea(uri) }}
      <opensilex-StringView
          label="component.common.name"
      ></opensilex-StringView>
      <opensilex-UriLink
          :to="{ path: '/area/details/' + encodeURIComponent(area.uri) }"
          :uri="area.uri"
          :value="area.name + ' (' + nameType().bold() + ')'"
          target="_blank"
      ></opensilex-UriLink>
    </template>
    <opensilex-StringView
        :value="authorName"
        label="component.area.details.author"
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

@Component
export default class AreaDetails extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
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
  showName;

  @Ref("areaForm") readonly areaForm!: any;

  area: AreaGetDTO = {
    uri: null,
    name: null,
    author: null,
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
          this.loadAuthor(this.area.author);
        })
        .catch(this.$opensilex.errorHandler);
  }

  loadAuthor(uriAuthor) {
    this.securityService
        .getUser(uriAuthor)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
          const {last_name, first_name} = http.response.result;
          this.authorName = first_name + " " + last_name;
        })
        .catch(this.$opensilex.errorHandler);
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
          .getService("opensilex.OntologyService")
          .getURILabel(this.rdf_type)
          .then((http: HttpResponse<OpenSilexResponse<string>>) => {
            this.area.rdf_type = http.response.result;
          })
          .catch(this.$opensilex.errorHandler);
    } else this.area.rdf_type = "";
  }

  private editArea() {
    this.areaService
        .getByURI(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          let form: any = http.response.result;
          this.areaForm.showEditForm(form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  private deleteArea() {
    this.areaService
        .deleteArea(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          let message =
              this.$i18n.t("component.area.title") +
              " " +
              http.response.result +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
          this.$opensilex.showSuccessToast(message);
        })
        .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style lang="scss" scoped>
.details-actions-row {
  margin-top: -35px;
  margin-left: -15px;
  margin-right: 15px;
}

::v-deep .capitalize-first-letter {
  display: block;
}

::v-deep a {
  color: #007bff;
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
      delete: Delete area
      details:
        uri: URI
        name: Name
        author: Author
        rdfType: Type
        description: Description
        geometry: Geometry
fr:
  component:
    area:
      title: Zone
      delete: Supprimer la zone
      details:
        uri: URI
        name: Nom
        author: Auteur
        rdfType: Type
        description: Description
        geometry: Géométrie
</i18n>