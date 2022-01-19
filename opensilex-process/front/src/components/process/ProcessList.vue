<template>
  <b-card>
    <!-- Card header -->
    <template v-slot:header>
      <h3>
        {{ $t("component.process.title") }}
        &nbsp;
        <font-awesome-icon
          icon="question-circle"
          v-b-tooltip.hover.top="$t('component.process.process-help')"
        />
      </h3>

      <div class="card-header-right">
        <!-- TO DO : CREDENTIAL
             v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
        <opensilex-CreateButton
          @click="processForm.showCreateForm()"
          label="component.process.add"
        ></opensilex-CreateButton>
      </div>
    </template>
    <!-- Card body -->
    <!-- Process name filter -->
    <opensilex-StringFilter
      :filter.sync="filter.name"
      @update="updateFilter()"
      placeholder="component.process.name-filter-placeholder"
    ></opensilex-StringFilter>

    <template v-slot>
        <opensilex-TableAsyncView
            ref="tableRef"
            :searchMethod="searchProcess"
            :fields="fields"
            defaultSortBy="name"
        >
        <template v-slot:cell(uri)="{data}">
        <opensilex-UriLink :uri="data.item.uri"
        :value="data.item.title"
        ></opensilex-UriLink>
        </template>

      <template v-slot:cell(actions)="{data}">
            <opensilex-DetailButton
            @click="showProcessDetail(data.item.uri)"
            label="component.process.showDetail"
            :small="true"
            ></opensilex-DetailButton>

            <!-- TO DO 
                v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
            <opensilex-EditButton
            @click="$emit('onEdit', data.item)"
            label="component.process.edit"
            :small="true"
            ></opensilex-EditButton>

            <!-- TO DO 
                v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
            <!-- <opensilex-Dropdown
                @click="createStep(data.item.uri)"
                :small="true"
                icon="fa#plus"
                variant="outline-success"
                right
            >
            </opensilex-Dropdown> -->

            <!-- TO DO 
                v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_DELETE_ID)" -->
            <opensilex-DeleteButton
            @click="deleteProcess(data.item.uri)"
            label="component.process.delete"
            :small="true"
            ></opensilex-DeleteButton>
            </template>
        </opensilex-TableAsyncView>
    </template>

    <!-- TO DO 
        v-if="user.hasCredential(credentials.CREDENTIAL_PROCESS_MODIFICATION_ID)" -->
    <opensilex-ModalForm
      ref="processForm"
      component="opensilex-process-ProcessForm"
      createTitle="component.process.add"
      editTitle="component.process.edit"
      icon="ik#ik-monitor"
      @onCreate="refresh()"
      @onUpdate="refresh($)"
    ></opensilex-ModalForm>
  </b-card>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
// @ts-ignore
import {ProcessGetDTO, ProcessService} from "./index";

@Component
export default class ProcessList extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $router: any;
  service: ProcessService;

  private langUnwatcher;

  private selectedProcess: ProcessGetDTO;

  @Ref("processForm") readonly processForm!: any;
  @Ref("tableRef") readonly tableRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private filter: any = "";

  updateFilter() {
    this.$opensilex.updateURLParameter("filter", this.filter, "");
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService(
        "opensilex-process.ProcessService"
    );

    let query: any = this.$route.query;
    if (query.filter) {
      this.filter = decodeURIComponent(query.filter);
    }

    this.refresh();
  }

  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        if (this.selectedProcess != null) {
          this.displayProcessDetail(this.selectedProcess.uri, true);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  refresh() {
    this.$opensilex.updateURLParameters(this.filter);
    this.tableRef.refresh();
  }

  public displayProcessDetail(uri: string, forceRefresh?: boolean) {
    if (forceRefresh || this.selectedProcess == null || this.selectedProcess.uri != uri) {
      return this.service
        .getProcess(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProcessGetDTO>>) => {
          let detailDTO: ProcessGetDTO = http.response.result;
          this.selectedProcess = detailDTO;
          this.$emit("onSelect", detailDTO);
        });
    }
  }

  showProcessDetail(uri) {
    this.$router.push({
      path: "/process/details/" + encodeURIComponent(uri),
    });
  }

  deleteProcess(uri: string) {
    this.service
      .deleteProcess(uri)
      .then(() => {
        this.refresh();
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
.sl-vue-tree-root {
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
}

.leaf-spacer {
  display: inline-block;
  width: 23px;
}

@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }
}

.tree-multiple-icon {
  padding-left: 8px;
  color: #3cc6ff;
}
</style>
