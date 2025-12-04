<template>
  <div>
    <opensilex-PageContent
        class="pagecontent"
    >
      <!-- Toggle Sidebar -->
      <div class="searchMenuContainer"
           v-on:click="$emit('toggleFilters', searchFiltersToggle)"
           :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>
      <Transition>
        <div v-show="searchFiltersToggle">
          <opensilex-SearchFilterField
              @search="refresh()"
              @clear="reset()"
              label="GeneticResourceGroupList.filter.description"
              :showAdvancedSearch="false"
              class="searchFilterField"
          >
            <template v-slot:filters>
              <!-- Name -->
              <div>
                <opensilex-FilterField>
                  <label>{{$t('GeneticResourceGroupList.filter.label')}}</label>
                  <opensilex-StringFilter
                      :filter.sync="filter.name"
                      placeholder="GeneticResourceGroupList.filter.label-placeholder"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></opensilex-StringFilter>
                </opensilex-FilterField> <br>
              </div>

              <!-- geneticResource -->
              <div>
                <opensilex-FilterField quarterWidth="false">
                  <opensilex-GeneticResourceSelectorWithFilter
                      :geneticResourcesUris.sync="filter.geneticResource"
                  ></opensilex-GeneticResourceSelectorWithFilter>
                </opensilex-FilterField>
              </div>
            </template>
          </opensilex-SearchFilterField>
        </div>
      </Transition>
      <opensilex-TableAsyncView
          ref="tableRef"
          :searchMethod="searchGeneticResourceGroup"
          defaultPageSize=15
          :fields="fields"
          :isSelectable="true"
          selectMode="single"
          @select="onItemSelected"
          @unselect="$emit('unselect', $event)"
          @refreshed="onRefreshed"
          defaultSortBy="name"
          labelNumberOfSelectedRow="GeneticResourceGroupList.selected"
          iconNumberOfSelectedRow="fa#seedling"
          class="tableFirstChildClass"
      >
        <template
            v-slot:cell(name)="{data}"
        >
          <span :style="{
              'font-weight': data.selected ? 'bold' : 'normal'
          }">{{ data.item.geneticResource_count ? data.item.name + ' ' + $tc('GeneticResourceGroupStructureList.geneticResource', data.item.geneticResource_count, { count: data.item.geneticResource_count }) : data.item.name }}</span>
        </template>

        <template v-slot:cell(actions)="{data}">
          <b-button-group size="sm" >
            <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
                @click="$emit('onEdit', data.item.uri)"
                label="GeneticResourceGroupList.update"
                :small="true"
            ></opensilex-EditButton>
            <opensilex-DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_DELETE_ID)"
                @click="$emit('onDelete', data.item.uri)"
                label="GeneticResourceGroupList.delete"
                :small="true"
            ></opensilex-DeleteButton>
          </b-button-group>
        </template>
      </opensilex-TableAsyncView>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import VueRouter, {Route} from "vue-router";
import { GeneticResourceService, GeneticResourceSearchFilter } from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OpenSilexStore} from "../../models/Store";
import TableAsyncView from "../common/views/TableAsyncView.vue";
import {GeneticResourceGroupGetDTO} from "opensilex-core/model/geneticResourceGroupGetDTO";

@Component
export default class GeneticResourceGroupList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;
  $route: Route;
  $router: VueRouter;
  service: GeneticResourceService;

  @Ref("tableRef") readonly tableRef!: TableAsyncView<GeneticResourceGroupGetDTO>;

  @Prop({
    default: false
  })
  isSelectable: boolean;

  @Prop({
    default: false
  })
  noActions: boolean;

  get user() {
    return this.$store.state.user;
  }

  get onlySelected() {
    return this.tableRef.onlySelected;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get lang() {
    return this.$store.state.lang;
  }

  @Prop()
  searchFiltersToggle: boolean;

  filter = {
    name: undefined,
    uri: undefined,
    geneticResource: []
  };

  reset() {
    this.filter = {
      name: undefined,
      uri: undefined,
      geneticResource: []
    };

    this.refresh();
  }

  onItemSelected(data : GeneticResourceGroupGetDTO) {
    this.tableRef.onRowClicked(data);
    this.$emit('select', data);
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {
          this.updateLang();
        }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.GeneticResourceService")
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
  }

  get fields() {
    let tableFields = [
      {
        key: "name",
        label: "GeneticResourceGroupList.name",
        sortable: false
      }
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions",
        sortable: false
      });
    }
    return tableFields;
  }

  refresh() {
    this.tableRef.selectAll = false;
    this.$opensilex.updateURLParameters(this.filter);

    if(this.tableRef.onlySelected) {
      this.tableRef.onlySelected = false;
    }
      this.tableRef.refresh();
  }

  searchGeneticResourceGroup(options) {
    return this.service.searchGeneticResourceGroups(this.filter.name, this.filter.geneticResource, options.orderBy, options.currentPage, options.pageSize);
  }

  updateLang() {
    this.refresh();
  }


  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

  onRefreshed() {
      this.$nextTick(()=>{if(this.tableRef.selectAll === true && this.tableRef.selectedItems.length !== this.tableRef.totalRow) {
          this.tableRef.selectAll = false;
      }})
  }
}
</script>

<style scoped lang="scss">
.clear-btn {
  color: rgb(229, 227, 227) !important;
  border-color: rgb(229, 227, 227) !important;
  border-left: none !important;
}

.group-list-button-placement {
  margin-right: 10px;

}

</style>

<i18n>

en:
  GeneticResourceGroupList:
    uri: URI
    name: Name
    update: Update group
    delete: Delete group
    selectLabel: Select genetic resource
    selected: Group list
    selected-all: All groups
    filter:
      description: Genetic Resource group search
      label: Name
      label-placeholder: Enter group name
      uri: URI
      uri-placeholder: Enter a part of an URI
      search: Search
      reset: Reset
fr:
  GeneticResourceGroupList:
    uri: URI
    label: Nom
    update: Modifier les ressources génétiques
    delete: Supprimer les ressources génétiques
    selectLabel: Sélection de ressources génétiques
    selected: Liste de groupes
    selected-all: Toutes les ressources génétiques
    filter:
      description: Recherche de groupe de ressources génétiques
      label: Nom
      label-placeholder: Entrer un nom de groupe
      uri: URI
      uri-placeholder: Entrer une partie d'une URI
      search: Rechercher
      reset: Réinitialiser

</i18n>
