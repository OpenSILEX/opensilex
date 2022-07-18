<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      :title="factor.name"
      description="component.menu.experimentalDesign.factors"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
          class="ml-3"
          :active="isDetailsTab()"
          :to="{
            path:
              '/' +
              encodeURIComponent(xpUri) +
              '/factor/details/' +
              encodeURIComponent(uri),
          }"
          >{{ $t("component.common.details-label") }}
        </b-nav-item>
        <b-nav-item
          :active="isExperimentTab()"
          :to="{
            path: '/' +
              encodeURIComponent(xpUri) +
              '/factor/experiments/' +
               encodeURIComponent(uri)
          }"
          >{{ $t("component.common.details.experiment") }}
        </b-nav-item>
        <b-nav-item
          :active="isDocumentTab()"
          :to="{
            path:
              '/' +
              encodeURIComponent(xpUri) +
              '/factor/document/' +
              encodeURIComponent(uri),
          }"
          >{{ $t("ExperimentView.document") }}
        </b-nav-item>
        <b-nav-item
          :active="isAnnotationTab()"
          :to="{
            path:
              '/' +
              encodeURIComponent(xpUri) +
              '/factor/annotations/' +
              encodeURIComponent(uri),
          }"
          >{{ $t("Annotation.list-title") }}
        </b-nav-item>
        <opensilex-AnnotationModalForm
          v-if="
            isAnnotationTab() &&
            user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
          "
          ref="annotationModalForm"
          :target="uri"
          @onCreate="updateAnnotations"
          @onUpdate="updateAnnotations"
        ></opensilex-AnnotationModalForm>
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-FactorDetails
          v-if="isDetailsTab()"
          @onUpdate="loadFactor(uri)"
          @onUpdateReferences="callUpdateFactorService"
          @onDelete="deleteFactor(uri)"
          @onReload="loadFactor(uri)"
          :factor="factor"
          :experiment="xpUri"
        ></opensilex-FactorDetails>
        <opensilex-AssociatedExperiments
          v-else-if="isExperimentTab()"
          :searchMethod="searchExperiments"
        ></opensilex-AssociatedExperiments>
        <opensilex-AnnotationList
          v-else-if="isAnnotationTab()"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="
            credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID
          "
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
          @onEdit="annotationModalForm.showEditForm($event)"
        ></opensilex-AnnotationList>

        <opensilex-DocumentTabList
          v-else-if="isDocumentTab()"
          :uri="uri"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
        ></opensilex-DocumentTabList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { FactorDetailsGetDTO, FactorUpdateDTO, FactorsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import AnnotationModalForm from "../../annotations/form/AnnotationModalForm.vue";
import AnnotationList from "../../annotations/list/AnnotationList.vue";

@Component
export default class FactorView extends Vue {
  uri = null;
  xpUri = null;
  $opensilex: any;
  $store: any;
  $route: any;
  $router: any;

  $t: any;
  $i18n: any;
  service: FactorsService;
  factor: any = {
    uri: null,
    name: null,
    category: null,
    description: null,
    exact_match: [],
    close_match: [],
    broader_match: [],
    narrower_match: [],
    factor_levels: [],
  };

  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("annotationModalForm")
  readonly annotationModalForm!: AnnotationModalForm;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  callUpdateFactorService(form: FactorUpdateDTO, done) {
    console.debug("callUpdateFactorService");
    if (form instanceof Promise) {
      form.then((factor) => {
        this.service
          .updateFactor(form)
          .then((http: HttpResponse<OpenSilexResponse<any>>) => {
            let uri = http.response.result;
            console.debug("Updated factor", uri);
            // this.$router.push({
            //   path: '/' + encodeURIComponent(this.xpUri) +  "/factor/details/" + encodeURIComponent(uri),
            // });
          });
      });
    } else {
      this.service
        .updateFactor(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Updated factor", uri);
          // this.$router.push({
          //   path: '/' + encodeURIComponent(this.xpUri) +  "/factor/details/" + encodeURIComponent(uri),
          // });
        });
    }
  }

  deleteFactor(uri: any) {
    console.debug("check Associated factor " + uri);
    this.service
      .deleteFactor(uri)
      .then(() => {
        let message =
          this.$i18n.t("component.factor.label") +
          " " +
          uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$router.push({
          path: "/experiment/factors/" + encodeURIComponent(this.xpUri),
        });
      })
      .catch((error) => {
        if (error.status === 400) {
          this.$opensilex.showErrorToast(
            this.$i18n.t("component.factor.isAssociatedTo")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.xpUri = decodeURIComponent(this.$route.params.xpUri);

    this.loadFactor(this.uri);
  }

  searchExperiments() {
    return this.service.getFactorAssociatedExperiments(this.uri);
  }

  loadFactor(uri: string) {
    this.service
      .getFactorByURI(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factor = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  isDetailsTab() {
    return this.$route.path.startsWith(
      "/" + encodeURIComponent(this.xpUri) + "/factor/details/"
    );
  }

  isExperimentTab() {
    return this.$route.path.startsWith(
      "/" + encodeURIComponent(this.xpUri) + "/factor/experiments/"
    );
  }

  isAnnotationTab() {
    return this.$route.path.startsWith(
      "/" + encodeURIComponent(this.xpUri) + "/factor/annotations/"
    );
  }

  isDocumentTab() {
    return this.$route.path.startsWith(
      "/" + encodeURIComponent(this.xpUri) + "/factor/document/"
    );
  }

  updateAnnotations() {
    this.$nextTick(() => {
      this.annotationList.refresh();
    });
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    component:
        factor:
            returnButton: Return to the factor list
            alert-delete: This factor is linked to existing experiments and can't be deleted


fr:
    component:
        factor:
            returnButton: Retourner à la liste des facteurs
            alert-delete: Ce facteur est lié à des expérimentations et ne peut-être supprimé

</i18n>
