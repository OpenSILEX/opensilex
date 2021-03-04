<template>
  <div v-if="area.uri" class="container-fluid">
    <opensilex-PageHeader
        :title="area.name"
        description="component.area.title"
        icon="fa#sun"
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
                    label="MapView.delete-area-button"
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
                  :value="nameType()"
                  label="component.area.details.rdfType"
              ></opensilex-StringView>
              <opensilex-StringView
                  :value="area.description"
                  label="component.area.details.description"
              ></opensilex-StringView>

              <button
                  :title="$t('component.area.copyGeometryWKT')"
                  class="uri-copy"
                  v-on:click.prevent.stop="copyGeometry(wktValue(area.geometry), 'WKT')"
              >
                {{ $t("component.area.geometryWKT") }}
                <b-badge>
                  <opensilex-Icon icon="ik#ik-copy"/>
                </b-badge>
              </button>
              <br/>
              <button
                  :title="$t('component.area.copyGeometryGeoJSON')"
                  class="uri-copy"
                  v-on:click.prevent.stop="copyGeometry(JSON.stringify(area.geometry),'GeoJSON')"
              >
                {{ $t("component.area.geometryGeoJSON") }}
                <b-badge>
                  <opensilex-Icon icon="ik#ik-copy"/>
                </b-badge>
              </button>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>

      <opensilex-DocumentTabList
          v-else-if="isDocumentTab()"
          ref="documentList"
          :deleteCredentialId="credentials.CREDENTIAL_AREA_DELETE_ID"
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
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {AreaGetDTO} from "opensilex-core/model/areaGetDTO";
import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
import copy from "copy-to-clipboard";
import {stringify} from "wkt";

@Component
export default class AreaDetails extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  service: any;

  @Ref("areaForm") readonly areaForm!: any;

  area: AreaGetDTO = {
    uri: null,
    name: null,
    rdf_type: null,
    description: null,
    geometry: null,
  };

  rdf_type: string;
  private uri: string;
  private lang: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  wktValue(geometry) {
    try {
      return stringify(geometry);
    } catch (error) {
      return "";
    }
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.AreaService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadArea();
  }

  loadArea() {
    this.service
        .getByURI(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<AreaGetDTO>>) => {
          this.area = http.response.result;
          this.rdf_type = this.area.rdf_type;
        })
        .catch(this.$opensilex.errorHandler);
  }

  nameType() {
    if (this.user.locale != this.lang) {
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

  copyGeometry(address, standard) {
    copy(address);

    if (standard == "GeoJSON") {
      this.$opensilex.showSuccessToast(
          this.$t("component.area.copiedGeometryGeoJSON")
      );
    } else {
      this.$opensilex.showSuccessToast(
          this.$t("component.area.copiedGeometryWKT")
      );
    }
  }

  private rdfTypeLabel() {
    this.$opensilex
        .getService("opensilex.OntologyService")
        .getURILabel(this.rdf_type)
        .then((http: HttpResponse<OpenSilexResponse<string>>) => {
          this.area.rdf_type = http.response.result;
        })
        .catch(this.$opensilex.errorHandler);
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
          let message =
              this.$i18n.t("component.area.title") +
              " " +
              http.response.result +
              " " +
              this.$i18n.t("component.common.success.delete-success-message");
          this.$opensilex.showSuccessToast(message);
          this.$router.push({path: "/"});
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
</style>
<i18n>
en:
  component:
    area:
      title: Area
      copyGeometryGeoJSON: Copy the geometry in GeoJSON format
      copiedGeometryGeoJSON: The geometry has been copied in GeoJSON format.
      geometryGeoJSON: Geometry in GeoJSON
      copyGeometryWKT: Copy the geometry in WKT format
      copiedGeometryWKT: The geometry has been copied in WKT format.
      geometryWKT: Geometry in WKT
      details:
        uri: URI
        name: Name
        rdfType: Type
        description: Description
        geometry: Geometry
fr:
  component:
    area:
      title: Zone
      copyGeometryGeoJSON: Copier la géométrie au format GeoJSON
      copiedGeometryGeoJSON: La géométrie a été copiée au format GeoJSON.
      geometryGeoJSON: Géométrie en GeoJSON
      copyGeometryWKT: Copier la géométrie au format WKT
      copiedGeometryWKT: La géométrie a été copiée au format WKT.
      geometryWKT: Géométrie en WKT
      details:
        uri: URI
        name: Nom
        rdfType: Type
        description: Description
        geometry: Géométrie
</i18n>