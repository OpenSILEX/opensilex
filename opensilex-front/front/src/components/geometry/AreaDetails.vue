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

        <b-nav-item
            :active="isDocumentTab()"
            :to="{ path: '/area/documents/' + encodeURIComponent(uri) }"
        >{{ $t("component.project.documents") }}
        </b-nav-item>

        <b-nav-item
            :active="isAnnotationTab()"
            :to="{ path: '/area/annotations/' + encodeURIComponent(uri) }"
        >{{ $t("Annotation.list-title") }}
        </b-nav-item>
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
    <opensilex-StringView
        v-if="area.description && isViewAllInformation"
        :value="area.description"
        label="component.area.details.description"
    ></opensilex-StringView>
    <opensilex-GeometryCopy v-if="isViewAllInformation" :value="area.geometry"></opensilex-GeometryCopy>
    <p>
      <a v-on:click="isViewAllInformation = !isViewAllInformation">{{
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
import { AreaGetDTO, EventGetDTO, ObjectUriResponse } from 'opensilex-core/index';
import { UserGetDTO } from 'opensilex-security/index';

@Component
export default class AreaDetails extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  service: any;

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
    this.service = this.$opensilex.getService("opensilex.AreaService");
    if (this.withBasicProperties)
      this.loadArea(decodeURIComponent(this.$route.params.uri));
    else
      this.loadArea(this.uri);
  }

  loadArea(uri) {
    this.service
        .getByURI(uri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          this.area = http.response.result;
          this.rdf_type = this.area.rdf_type;
          this.loadAuthor(this.area.author);
        })
        .catch(this.$opensilex.errorHandler);
  }

  loadAuthor(uriAuthor) {
    this.$opensilex
        .getService("opensilex.SecurityService")
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
    this.$opensilex
        .getService("opensilex.AreaService")
        .getByURI(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          let form: any = http.response.result;
          this.areaForm.showEditForm(form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  private deleteArea() {
    this.$opensilex
        .getService("opensilex.AreaService")
        .deleteArea(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
          if (this.rdf_type == "vocabulary:TemporalArea") {
            return this.deleteEvent();
          }
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

  private deleteEvent() {
    const eventsService = "EventsService";
    this.$opensilex
      .getService(eventsService)
      .searchEvents(undefined, undefined, undefined, this.uri)
      .then((http: HttpResponse<OpenSilexResponse<EventGetDTO>>) => {
        const res = http.response.result[0] as any;
        this.$opensilex
          .getService(eventsService)
          .deleteEvent(res.uri)
          .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
            let message =
              this.$i18n.t("component.area.title") +
              " " +
              this.uri +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
              console.debug("Event deleted");
            this.$opensilex.showSuccessToast(message);
          })
          .catch(this.$opensilex.errorHandler);
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

#vl-overlay-detailItem > div > div > div > div > p > a {
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