<template>
  <div>
    <opensilex-SearchFilterField
      v-if="searchBar"
      @clear="resetSearch()"
      @search="updateFilters()"
      label="component.factor.list.filter.label"
    >
      <template v-slot:filters>
        <opensilex-FilterField>
          <b-form-group>
            <label for="name">{{
              $t("component.factor.list.filter.name")
            }}</label>
            <opensilex-StringFilter
              id="name"
              :filter.sync="filter.name"
              placeholder="component.factor.name-placeholder"
            ></opensilex-StringFilter>
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

        <opensilex-FilterField v-if="experiment == null">
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
      :isSelectable="isSelectable"
      labelNumberOfSelectedRow="FactorList.selected"
      iconNumberOfSelectedRow="ik#ik-feather"
    >
      <template v-slot:head(name)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:head(description)="{ data }">{{
        $t(data.label)
      }}</template>
      <template v-slot:head(category)="{ data }">{{ $t(data.label) }}</template>

      <!-- <template v-slot:head(uri)="{data}">{{$t(data.label)}}</template> -->
      <template v-slot:head(actions)="{ data }">{{ $t(data.label) }}</template>
      <template v-slot:cell(name)="{ data }">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{ path: '/' +encodeURIComponent(experiment) + '/factor/details/' + encodeURIComponent(data.item.uri) }"
        ></opensilex-UriLink>
      </template>
      <template v-slot:cell(category)="{ data }"
        ><span class="capitalize-first-letter">{{
          $opensilex.getFactorCategoryName(data.value)
        }}</span></template
      >
      <template v-slot:cell(actions)="{ data }">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
            "
            :small="true"
            label="component.common.list.buttons.update"
            @click="$emit('onEdit', data.item.uri)"
          ></opensilex-EditButton>
          <opensilex-InteroperabilityButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)
            "
            :small="true"
            label="component.common.list.buttons.interoperability"
            @click="$emit('onInteroperability', data.item.uri)"
          ></opensilex-InteroperabilityButton>
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID)"
            :small="true"
            label="component.common.list.buttons.delete"
            @click="$emit('onDelete', data.item)"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import { FactorsService, FactorCategoryGetDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class FactorList extends Vue {
  $opensilex: any;
  service: FactorsService;
  $store: any;
  $route: any;
  categories: any = {};

  @Prop({
    default: null,
  })
  experiment: string;

  @Prop({
    default: false,
  })
  isSelectable;

  @Prop({
    default: false,
  })
  noActions;

  @Prop({
    default: false,
  })
  searchBar;

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        this.$opensilex.loadFactorCategories().then(() => {
          this.refresh();
        });
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  filter = {
    uri: "",
    name: "",
    description: "",
    experiment: null,
    category: "",
  };

  resetSearch() {
    this.resetFilters();
    this.refresh();
  }

  resetFilters() {
    this.filter = {
      uri: null,
      name: "",
      description: "",
      experiment: null,
      category: "",
    };
    // Only if search and reset button are use in list
    if (this.experiment != null) {
      this.filter.experiment = this.experiment;
    }
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
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
  }

  updateFilters(value: string) {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");
    }
    this.refresh();
  }

  get fields() {
    let tableFields: any = [
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
        key: "description",
        label: "component.factor.list.description",
        sortable: false,
      },
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions",
      });
    }
    return tableFields;
  }

  @Ref("tableRef") readonly tableRef!: any;

  refresh() {
    this.tableRef.refresh();
  }

  getSelected() {
    return this.tableRef.getSelected();
  }

  searchFactors(options) {
    return this.service.searchFactors(
      this.$opensilex.prepareGetParameter(this.filter.name), // name
      this.$opensilex.prepareGetParameter(this.filter.description), // description
      this.$opensilex.prepareGetParameter(this.filter.category), // category
      this.$opensilex.prepareGetParameter(this.filter.experiment), // experiment
      options.orderBy, // orderBy
      options.currentPage, // page
      options.pageSize // pageSize
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
  FactorList:
    selected: Selected factor
    selectLabel: Select factor
  component: 
    factor: 
      list:
        name: Name
        category: Category
        description: description
        filter: 
          label: Filter factors
          name: Name
          experiment: Experiment
          category: Category
          name-placeholder: Irrigation, Shading, ...
          experiment-placeholder: Select experiment
            
fr:
  FactorList:
    selected: Facteur(s) séléectionné(s)
    selectLabel: Sélection de facteurs
  component: 
    factor: 
      list:
        name: Nom
        category: Categorie
        description: description
        filter: 
          label: Filtrer les facteurs
          name: Nom
          category: Catégorie
          experiment: Expérimentation
          name-placeholder: Irrigation, Ombrage, ...
          experiment-placeholder: Sélectionner experimentation

</i18n>