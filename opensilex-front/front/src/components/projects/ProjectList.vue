<template>
  <div>
    <opensilex-PageContent
      class="pagecontent"
    >
           <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="ik ik-search"></i>
        </div>
      </div>
        <!-- FILTERS -->
      <Transition>
        <div v-show="SearchFiltersToggle">
    <opensilex-SearchFilterField
    @clear="reset()"
    @search="refresh()"
    class="searchFilterField">
      <template v-slot:filters>

        <!-- Name -->
        <div>
        <opensilex-FilterField>
          <label for="name">{{ $t("component.common.name") }}</label>
          <opensilex-StringFilter
            id="name"
            :filter.sync="filter.name"
            placeholder="component.project.filter-label-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter>
        </opensilex-FilterField><br>
        </div>

          <!-- Year -->
        <div>
        <opensilex-FilterField>
          <label>{{ $t("component.common.year") }}</label>
          <opensilex-StringFilter
            placeholder="component.project.filter-year-placeholder"
            :filter.sync="filter.year"
            type="number"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter>
        </opensilex-FilterField><br>
        </div>

        <!-- Keyword -->
        <div>
        <opensilex-FilterField>
          <label for="term">{{ $t("component.common.keyword") }}</label>
          <opensilex-StringFilter
            id="term"
            :filter.sync="filter.keyword"
            placeholder="component.project.filter-keywords-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter>
        </opensilex-FilterField><br>
        </div>

        <!-- Financial Funding -->
        <div>
        <opensilex-FilterField>
          <label for="financial">{{ $t("component.project.financialFunding") }}</label>
          <opensilex-StringFilter
            id="financial"
            :filter.sync="filter.financial"
            placeholder="component.project.filter-financial-placeholder"
            class="searchFilter"
            @handlingEnterKey="refresh()"
          ></opensilex-StringFilter>
        </opensilex-FilterField><br>
        </div>
        
      </template>
    </opensilex-SearchFilterField>
            </div>
        </Transition>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      defaultSortBy="start_date"
      :defaultPageSize="pageSize"
      :isSelectable="true"
      labelNumberOfSelectedRow="component.project.selectedLabel"
      @select="$emit('select', $event)"
      @unselect="$emit('unselect', $event)"
      @selectall="$emit('selectall', $event)"
      @refreshed="onRefreshed"
    >
      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
        
        <b-dropdown
          dropright
          class="mb-2 mr-2"
          :small="true"
          :text="$t('VariableList.display')">

          <b-dropdown-item-button @click="clickOnlySelected()">{{ onlySelected ? $t('ProjectList.selected-all') : $t("component.common.selected-only")}}</b-dropdown-item-button>
          <b-dropdown-item-button @click="resetSelected()">{{$t("component.common.resetSelected")}}</b-dropdown-item-button>
        </b-dropdown>

        <b-dropdown
          v-if="!noActions" 
          dropright
          class="mb-2 mr-2"
          :small="true"
          :disabled="numberOfSelectedRows == 0"
          text="actions"
        >
          <b-dropdown-item-button @click="createDocument()">{{$t('component.common.addDocument')}}</b-dropdown-item-button>          
        </b-dropdown>
      </template>
      <template v-slot:cell(name)="{ data }">
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{
            path: '/project/details/' + encodeURIComponent(data.item.uri),
          }"
        ></opensilex-UriLink>
      </template>

      <template v-slot:cell(start_date)="{ data }">
        <opensilex-DateView :value="data.item.start_date"></opensilex-DateView>
      </template>

      <template v-slot:cell(end_date)="{ data }">
        <opensilex-DateView :value="data.item.end_date"></opensilex-DateView>
      </template>

      <template v-slot:cell(state)="{ data }">
        <i
          v-if="!isEnded(data.item)"
          class="ik ik-activity badge-icon badge-info-opensilex"
          :title="$t('component.project.common.status.in-progress')"
        ></i>
        <i
          v-else
          class="ik ik-archive badge-icon badge-light"
          :title="$t('component.project.common.status.finished')"
        ></i>
      </template>

      <template v-slot:cell(actions)="{ data }">
        <b-button-group size="sm">
          <opensilex-EditButton
            v-if="
              user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)
            "
            @click="$emit('onEdit', data.item)"
            label="component.project.update"
            :small="true"
          ></opensilex-EditButton>

          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_DELETE_ID)"
            @click="deleteProject(data.item.uri)"
            label="component.project.delete"
            :small="true"
          ></opensilex-DeleteButton>
        </b-button-group>
      </template>
    </opensilex-TableAsyncView>

    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      createTitle="component.common.addDocument"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
    ></opensilex-ModalForm>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {ProjectsService} from "opensilex-core/index";

@Component
export default class ProjectList extends Vue {
  $opensilex: any;
  $store: any;

  service: ProjectsService;
  SearchFiltersToggle: boolean = false;

  @Ref("documentForm") readonly documentForm!: any;
  @Ref("tableRef") readonly tableRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get onlySelected() {
    return this.tableRef.onlySelected;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop({
    default: false
  })
  isSelectable;

  @Prop({
    default: false
  })
  noActions;

  @Prop({
    default: 20
  })
  pageSize: number;
  
  @Prop({
    default: false
  })
  noUpdateURL;

  filter = {
    year: undefined,
    name: "",
    keyword: "",
    financial: ""
    };
 

  reset() {
    this.filter = {
      year: undefined,
      name: "",
      keyword: "",
      financial: "",
    };
    this.refresh();
  }

  clickOnlySelected() {
    this.tableRef.clickOnlySelected();
  }

  resetSelected() {
    this.tableRef.resetSelected();
  }

  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter);
  }

  get fields() {
    let tableFields: any = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true
      },
      {
        key: "shortname",
        label: "component.project.shortname",
        sortable: true
      },
      {
        key: "start_date",
        label: "component.common.startDate",
        sortable: true
      },
      {
        key: "end_date",
        label: "component.common.endDate",
        sortable: true
      },
      {
        key: "financial_funding",
        label: "component.project.financialFunding",
        sortable: true
      },
      {
        key: "state",
        label: "component.common.state"
      }
    ];
    if (!this.noActions) {
      tableFields.push({
        key: "actions",
        label: "component.common.actions"
      });
    }
    return tableFields;
  }

  getSelected() {
    return this.tableRef.getSelected();
  }


  refresh() {
    if(this.tableRef.onlySelected) {
      this.tableRef.onlySelected = false;
      this.tableRef.changeCurrentPage(1);
    } else {
      this.tableRef.changeCurrentPage(1);
    }
    this.$nextTick(() => {
      if (!this.noUpdateURL) {
        this.$opensilex.updateURLParameters(this.filter);
      }
    });
  }

  updateSelectedProject(){
    if(this.tableRef.onlySelected) {
      this.tableRef.onlySelected = false;
    }
    this.$nextTick(() => {
      if (!this.noUpdateURL) {
        this.$opensilex.updateURLParameters(this.filter);
      }
    });
  }

  loadData(options) {
    return this.service.searchProjects(
      this.filter.name,
      this.filter.year,
      this.filter.keyword,
      this.filter.financial,
      options.orderBy,
      options.currentPage,
      options.pageSize      
    );
  }

  isEnded(project) {
    if (project.end_date) {
      return new Date(project.end_date).getTime() < new Date().getTime();
    }
    return false;
  }

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }
  onItemSelected(row) {
    this.tableRef.onItemSelected(row);
  }

  deleteProject(uri: string) {
    this.service
      .deleteProject(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
        let message = this.$i18n.t("ProjectList.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
      })
      .catch(this.$opensilex.errorHandler);
  }

  createDocument() {
    this.documentForm.showCreateForm();
  }

  initForm() {
    let targetURI = [];
    for (let select of this.tableRef.getSelected()) {
      targetURI.push(select.uri);
    }

    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: targetURI,
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
    };
  }

  onRefreshed() {
    let that = this;
    setTimeout(function() {
      if(that.tableRef.selectAll === true && that.tableRef.selectedItems.length !== that.tableRef.totalRow) {                    
        that.tableRef.selectAll = false;
      } 
    }, 1);
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ProjectList:
    name: The project
    selected-all: All projects

fr:
  ProjectList:
    name: Le projet
    selected-all: Tout les projets

</i18n>

