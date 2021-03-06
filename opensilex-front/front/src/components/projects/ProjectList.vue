<template>
  <div>
    <opensilex-SearchFilterField @clear="reset()" @search="refresh()">
      <template v-slot:filters>
        <opensilex-FilterField>
          <label for="name">{{ $t("component.common.name") }}</label>
          <opensilex-StringFilter
            id="name"
            :filter.sync="filter.name"
            placeholder="component.project.filter-label-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <label>{{ $t("component.common.year") }}</label>
          <opensilex-StringFilter
            placeholder="component.project.filter-year-placeholder"
            :filter.sync="filter.year"
            type="number"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <label for="term">{{ $t("component.common.keyword") }}</label>
          <opensilex-StringFilter
            id="term"
            :filter.sync="filter.keyword"
            placeholder="component.project.filter-keywords-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <label for="financial">{{ $t("component.project.financialFunding") }}</label>
          <opensilex-StringFilter
            id="financial"
            :filter.sync="filter.financial"
            placeholder="component.project.filter-financial-placeholder"
          ></opensilex-StringFilter>
        </opensilex-FilterField>
        
      </template>
    </opensilex-SearchFilterField>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="loadData"
      :fields="fields"
      defaultSortBy="start_date"
      :isSelectable="true"
      :maximumSelectedRows="maximumSelectedRows"
      labelNumberOfSelectedRow="component.project.selectedLabel"
    >
      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
        <b-dropdown
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
          v-if="!noActions"
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
  </div>
</template>

<script lang="ts">
import moment from "moment";
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProjectsService } from "opensilex-core/index";

@Component
export default class ProjectList extends Vue {
  $opensilex: any;
  $store: any;

  service: ProjectsService;

  @Ref("documentForm") readonly documentForm!: any;

  get user() {
    return this.$store.state.user;
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

  @Prop()
  maximumSelectedRows;

  filter = {
    year: undefined,
    name: "",
    keyword: "",
    financial: "",
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
  
  updateFiltersFromURL() {
    let query: any = this.$route.query;
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        if (Array.isArray(this.filter[key])){
          this.filter[key] = decodeURIComponent(query[key]).split(",");
        } else {
          this.filter[key] = decodeURIComponent(query[key]);
        }        
      }
    }
  }

  updateURLFilters() {
    for (let [key, value] of Object.entries(this.filter)) {
      this.$opensilex.updateURLParameter(key, value, "");       
    }    
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.ProjectsService");
    this.updateFiltersFromURL();
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

  @Ref("tableRef") readonly tableRef!: any;
  refresh() {
    this.tableRef.selectAll = false;
    this.tableRef.onSelectAll();
    this.updateURLFilters();
    this.tableRef.refresh();
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
      return moment(project.end_date, "YYYY-MM-DD").diff(moment()) < 0;
    }
    return false;
  }

  onItemUnselected(row) {
    this.tableRef.onItemUnselected(row);
  }

  deleteProject(uri: string) {
    this.service
      .deleteProject(uri)
      .then(() => {
        this.refresh();
        this.$emit("onDelete", uri);
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
}
</script>

<style scoped lang="scss">
</style>

