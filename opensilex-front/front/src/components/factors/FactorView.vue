<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      :title="factor.name"
      description="component.menu.experimentalDesign.factors"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
          class="ml-3"
          :active="isDetailsTab()"
          :to="{ path: '/factor/details/' + encodeURIComponent(uri) }"
          >{{ $t("component.factor.details.label") }}</b-nav-item
        >
        <b-nav-item
          :active="isExperimentTab()"
          :to="{ path: '/factor/experiments/' + encodeURIComponent(uri) }"
          >{{ $t("component.common.details.experiment") }}</b-nav-item
        >
        <!-- <b-nav-item
          :active="false"
          :disabled="true"
          :to="{path: '/factor/document/' + encodeURIComponent(uri)}"
        >{{ $t('component.common.details.document') }}</b-nav-item> -->
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-FactorDetails
          v-if="isDetailsTab()"
          @onUpdate="loadFactor(uri)"
          @onUpdateReferences="callUpdateFactorService"
          @onDelete="deleteFactor(uri)"
          :factor="factor"
        ></opensilex-FactorDetails>
        <opensilex-AssociatedExperiments
          v-else-if="isExperimentTab()"
          :uri="uri"
        ></opensilex-AssociatedExperiments>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  FactorDetailsGetDTO,
  FactorUpdateDTO,
  FactorsService,
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FactorView extends Vue {
  uri = null;
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
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
    factorLevels: [],
  };

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
            this.$router.push({
              path: "/factor/details/" + encodeURIComponent(uri),
            });
          });
      });
    } else {
      this.service
        .updateFactor(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("Updated factor", uri);
          this.$router.push({
            path: "/factor/details/" + encodeURIComponent(uri),
          });
        });
    }
  }

  deleteFactor(uri: string) {
    console.debug("check Associated factor " + uri);
    let isAssociated = this.$opensilex
      .getService("opensilex.FactorsService")
      .getFactorAssciatedExperiments(uri)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        if (http.response.metadata.pagination.totalCount > 0) {
          this.$opensilex.showErrorToast(
            this.$i18n.t("component.factor.isAssociatedTo")
          );
        } else {
          console.debug("deleteFactor " + uri);
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
              this.$router.go(-1);
            })
            .catch(this.$opensilex.errorHandler);
        }
      });
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadFactor(this.uri);
  }

  loadFactor(uri: string) {
    this.service
      .getFactor(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factor = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/factor/details/");
  }

  isExperimentTab() {
    return this.$route.path.startsWith("/factor/experiments/");
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
 
            
fr:
  component: 
    factor:
      returnButton: Retourner à la liste des facteurs
</i18n>
