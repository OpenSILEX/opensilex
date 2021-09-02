<template>
  <b-form id="cluster-data-form" >
    <!-- Variable -->
    <opensilex-UsedVariableSelector
      ref="varSelector"
      label="Variable"
      :required="true"
      :multiple="false"
      :variables.sync="form.variable"
      :experiment="experiment"
    ></opensilex-UsedVariableSelector>
    
    <!-- Method -->
    <opensilex-SelectForm
      label="ClusterForm.method"
      placeholder="ClusterForm.placeholder-method"
      :required="true"
      :multiple="false"
      :options="methods"
      :selected.sync="form.method"
    ></opensilex-SelectForm>

    <!-- Number of clusters -->
    <opensilex-InputForm
      :value.sync="form.nbClusters"
      label="ClusterForm.nb-clusters"
      type="number"
      rules="integer|min_value:2|max_value:20"
      :required="true"
    ></opensilex-InputForm>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
// @ts-ignore
import {GermplasmGetSingleDTO} from "opensilex-core/model/germplasmGetSingleDTO";
// @ts-ignore
import {FactorDetailsGetDTO} from "opensilex-core/model/factorDetailsGetDTO";
// @ts-ignore
import {ScientificObjectNodeDTO} from "opensilex-core/index";

@Component
export default class ClusterForm extends Vue {
  featureOS: any[];

  experiment: string;

  @Prop()
  editMode;
  @Prop()
  title: string;

  form = {
    variable: null,
    method: "K-means",
  };

  methods: any[] = [
    { id: "K-means", label: "K-means"}
  ];

  feature: any[] = [];

  $opensilex: any;
  $store: any;

  get user() {
    return this.$store.state.user;
  }

  get lang() {
    return this.$store.state.lang;
  }

  reset() {
    this.form = {
      variable: null,
      method: "K-means",
    };
  }

  created() {
    this.$opensilex.showLoader();

    this.experiment = decodeURIComponent(this.$route.params.uri);
  }

  create(form) {
    if (this.form.variable == null) {
      return false;
    }
    return this.form;
  }

  update(form) {
    return null;
  }

  getEmptyForm() {
    return {
      variable: "K-means"
    };
  }
}
</script>

<style lang="scss" scoped>
div.col {
  display: inline;
}
</style>

<i18n>
en:
  ClusterForm:
    method: Method
    placeholder-method: Select one method
    nb-clusters: Number of clusters
fr:
  ClusterForm:
    method: Méthode
    placeholder-method: Sélectionnez une méthode
    nb-clusters: Nombre de clusters
</i18n>
