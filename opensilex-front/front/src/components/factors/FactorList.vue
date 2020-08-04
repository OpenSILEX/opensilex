<template>
  <div>
    <opensilex-SearchFilterField
      @clear="resetSearch()"
      @search="updateFilters()"
      label="component.factor.list.filter.label"
    >
      <template v-slot:filters>
        <opensilex-FilterField>
          <b-form-group>
            <label>{{$t('component.factor.list.filter.name')}}</label>
            <b-input-group>
              <b-form-input
                v-model="filter.name"
                type="text"
                default="null"
                :placeholder="$t('component.factor.name-placeholder')"
              ></b-form-input>
              <template v-slot:append>
                <b-btn variant="primary" @click="updateFilters(filter.name = null)">
                  <font-awesome-icon icon="times" size="sm" />
                </b-btn>
              </template>
            </b-input-group>
          </b-form-group>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <b-form-group>
            <b-input-group>
              <!-- Factor categories -->
              <opensilex-FactorCategorySelector
                label="component.factor.list.filter.category"
                placeholder="component.factor.names.category-placeholder"
                :category.sync="filter.category"
              ></opensilex-FactorCategorySelector>
            </b-input-group>
          </b-form-group>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <b-form-group>
            <b-input-group>
              <!-- Experiments -->
              <opensilex-ExperimentSelector
                label="component.factor.list.filter.experiment"
                :multiple="false"
                :experiments.sync="filter.experiment"
              ></opensilex-ExperimentSelector>
            </b-input-group>
          </b-form-group>
        </opensilex-FilterField>
      </template>
    </opensilex-SearchFilterField>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchFactors"
      :fields="fields"
      defaultSortBy="name"
    >
      <template v-slot:head(name)="{data}">{{$t(data.label)}}</template>
      <template v-slot:head(comment)="{data}">{{$t(data.label)}}</template>
      <template v-slot:head(category)="{data}">{{$t(data.label)}}</template>

      <!-- <template v-slot:head(uri)="{data}">{{$t(data.label)}}</template> -->
      <template v-slot:head(actions)="{data}">{{$t(data.label)}}</template>
      <template v-slot:cell(name)="{data}">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{path: '/factor/details/'+ encodeURIComponent(data.item.uri)}"
        ></opensilex-UriLink>
      </template>
      <template v-slot:cell(category)="{data}">{{$t(getCategoryLabel(data.value))}}</template>
      <template v-slot:cell(actions)="{data}">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
            :small="true"
            label="component.factor.list.update"
            @click="$emit('onEdit', data.item.uri)"
          ></opensilex-EditButton>
          <opensilex-InteroperabilityButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
            :small="true"
            label="component.factor.list.interoperrability"
            @click="$emit('onInteroperability', data.item.uri)"
          ></opensilex-InteroperabilityButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID)"
            :small="true"
            label="component.factor.list.delete"
            @click="$emit('onDelete', data.item.uri)"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import {
  FactorsService,
  FactorGetDTO,
  FactorSearchDTO,
} from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";
import { FactorCategory } from "./FactorCategory";

@Component
export default class FactorList extends Vue {
  $opensilex: any;
  service: FactorsService;
  $store: any;
  $route: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  getCategoryLabel(category) {
    return FactorCategory.getFactorCategoryLabel(category);
  }

  filter: FactorSearchDTO = {
    uri: "",
    name: "",
    comment: "",
    experiment: null,
    category: ""
  };

  resetSearch() {
    this.resetFilters();
    this.refresh();
  }

  resetFilters() {
    this.filter = {
      uri: null,
      name: "",
      comment: "",
      experiment: null,
      category: "",
    };
    // Only if search and reset button are use in list
  }

  onDetails(uri) {
    this.$emit("onDetails", uri);
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
    let query: any = this.$route.query;

    this.resetFilters();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURI(query[key]);
      }
    }
  }

  updateFilters(value: string) {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  fields = [
    // {
    //   key: "uri",
    //   label: "component.factor.list.uri",
    //   sortable: true
    // },
    {
      key: "name",
      label: "component.factor.list.name",
      sortable: true,
    },
    {
      key: "category",
      label: "component.factor.list.category",
      sortable: true,
    },
    {
      key: "comment",
      label: "component.factor.list.comment",
      sortable: false,
    },
    {
      key: "actions",
      label: "component.common.actions",
    },
  ];
  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  searchFactors(options) {
    return this.service.searchFactors(
      options.orderBy,
      options.currentPage,
      options.pageSize,
      this.filter
    );
  }
}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>

<i18n>

en:
  component: 
    factor: 
      list:
        uri: Factor URI
        name: Name
        category: Category
        comment: Comment
        filter: 
          label: Filter factors
          name: By name
          experiment: By experiment
          category: By category
          name-placeholder: Irrigation, Shading, ...
          experiment-placeholder: Select experiment
            
fr:
  component: 
    factor: 
      list:
        uri: URI du facteur
        name: Nom
        category: Categorie
        comment: Commentaire
        filter: 
          label: Filtrer les facteurs
          name: Par nom
          category: Par catégorie
          experiment: Par expérimentationn
          name-placeholder: Irrigation, Ombrage, ...
          experiment-placeholder: Sélectionner experimentation

</i18n>