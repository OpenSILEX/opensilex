<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      title="component.menu.experimentalDesign.factors"
      :description="factor.name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" :returnTo="goBack()" :returnToTitle="returnTitle()">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :disabled="!isDetailsTab()"
          :to="{path: '/factor/details/' + encodeURIComponent(uri)}"
        >{{ $t('component.factor.details.label') }}</b-nav-item>
        <b-nav-item
          :active="false"
          :disabled="true"
          :to="{path: '/factor/document/' + encodeURIComponent(uri)}"
        >{{ $t('component.common.details.document') }}</b-nav-item>
        <b-nav-item
          :active="false"
          :disabled="true"
          :to="{path: '/factor/experiment/' + encodeURIComponent(uri)}"
        >{{ $t('component.common.details.experiment') }}</b-nav-item>
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-FactorDetails v-if="isDetailsTab()" :factor="factor"></opensilex-FactorDetails>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { FactorDetailsGetDTO, FactorsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FactorView extends Vue {
  uri = null;
  createdFactor = false;
  $opensilex: any;
  $store: any;
  $route: any;
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
        factorLevels: []
      };

  get user() {
    return this.$store.state.user;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    this.uri = this.$route.params.uri;
    if (this.$route.query.created != null) {
      this.createdFactor = this.$route.query.created;
    }
    let uri = this.$route.params.uri;
    this.loadFactor(uri);
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

  goBack() {
    // return to the previous page
    if (!this.createdFactor) {
      return false;
    } else {
      // return to the list if factor has just been created
      return "/factors";
    }
  }
  
  returnTitle() {
    // return to the previous page
    if (this.createdFactor) {
      // return to the list if factor has just been created
      return "component.factor.returnButton";
    }
  }
}
</script>

<style scoped lang="scss">
.back-button {
  margin-right: 15px;
  height: 37px;
  display: flex;
  justify-content: center;
  align-items: center;
}
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
