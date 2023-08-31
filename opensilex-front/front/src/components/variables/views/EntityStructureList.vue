<template>
  <b-card>
    <!-- Card body -->
    <!-- Filter -->
    <opensilex-StringFilter
        :filter.sync="prefLabelFilter"
        @update="updateFilter()"
        placeholder="EntityList.filter-placeholder"
    ></opensilex-StringFilter>

    <opensilex-TreeView :nodes.sync="nodes" @select="displayNodesDetail">
      <template v-slot:node="{ node }">
                <span class="item-icon">
                    <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.rdf_type)"/>
                </span>&nbsp;
        <!-- <strong v-if="node.data.selected">{{ node.title }}</strong> -->
        <strong v-if="node.data.selected">
            {{ node.data.prefLabels[$i18n.locale]            }}</strong>
        <!-- <span v-if="!node.data.selected">{{ node.title }}</span> -->
        <span v-if="!node.data.selected">{{
            node.data.prefLabels[$i18n.locale]           }}</span>
      </template>
      <template v-slot:buttons="{ node }">
        <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
            @click="edit(node.data.uri)"
            label="Edit"
            :small="true"
        ></opensilex-EditButton>

      </template>
    </opensilex-TreeView>
  </b-card>
</template>

<script lang="ts">
import {Component, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import VariablesView from "../VariablesView.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
// @ts-ignore
import {VariablesService, MultiNamedResourceDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";

@Component
export default class EntityStructureList extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $route: any;
  service: VariablesService;
  $i18n: any;

  private selectedEntity : any;
  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  private prefLabelFilter: any = "";

  updateFilter() {
    this.$opensilex.updateURLParameter("prefLabel", this.prefLabelFilter, "");
    this.refresh(false);
  }

  created() {
    this.service = this.$opensilex.getService("opensilex-core.VariablesService");

    let query: any = this.$route.query;
    if (query && query.selectedEntity) {
      this.refresh(false, decodeURIComponent(query.selectedEntity));
    } else {
      this.refresh(false);
    }
  }

  mounted() {
    this.$store.watch(
        () => this.$store.getters.language,
        lang => {
          if (this.selectedEntity != null) {
            this.displayNodeDetail(this.selectedEntity.uri, true);
          }
        }
    );
  }

  searchElements(prefLabelFilter: string): Promise<HttpResponse<OpenSilexResponse<Array<MultiNamedResourceDTO>>>> {

    let orderByEntity = ["prefLabel=asc"];
    return this.service.searchEntities(prefLabelFilter, orderByEntity);

  }

  refresh(updateType: boolean, uri?) {

    if (updateType) {
      this.prefLabelFilter = "";
    }

    this.searchElements(this.prefLabelFilter)
        .then((http: HttpResponse<OpenSilexResponse<Array<MultiNamedResourceDTO>>>) => {
          let treeNode = [];
          let first = true;
          let uriFoundInSearch = false;
          for (let i in http.response.result) {
            let resourceTree: MultiNamedResourceDTO = http.response.result[i];
            let node = this.dtoToNode(resourceTree, first, uri);
            treeNode.push(node);

            if (uri == null) {
              if (first) {
                first = false;
              }
            } else {
              if (!uriFoundInSearch && this.$opensilex.checkURIs(resourceTree.uri, uri)) {
                uriFoundInSearch = true;
              }
            }
          }

          if (uri != null) {
            // display the node detail in head on list, only if the node is not already included into the search results
            this.displayNodeDetail(uri, true, !uriFoundInSearch);
          }

          if (http.response.result.length == 0) {
            this.selectedEntity = null;
          }

          this.nodes = treeNode;
        }).catch(this.$opensilex.errorHandler);

    if (this.nodes.length > 0) {

      this.selectedEntity = this.nodes[0].data;

    }
  }

  private dtoToNode(dto: MultiNamedResourceDTO, first: boolean, uri: string) {
    let isLeaf = true;

    let childrenDTOs = [];

    let isSelected = first && uri == null;
    if (uri != null) {
      isSelected = uri == dto.uri;
    }
    return {
      title: dto.prefLabels[this.$i18n.locale],
      data: dto,
      isLeaf: isLeaf,
      children: childrenDTOs,
      isExpanded: true,
      isSelected: isSelected,
      isDraggable: false,
      isSelectable: true
    };
  }

  public nodes = [];


  public displayNodesDetail(node: any) {

    this.displayNodeDetail(node.data.uri);

  }

  private getDetails(uri: string): Promise<HttpResponse<OpenSilexResponse>> {

    return this.service.getEntity(uri);

  }

  public displayNodeDetail(uri: string, forceRefresh?: boolean, appendNodeToNodeList?: boolean) {
    if ((forceRefresh || this.selectedEntity == null || this.selectedEntity.uri != uri) ) {
      return this.getDetails(uri)
          .then((http: HttpResponse<OpenSilexResponse>) => {
            if (appendNodeToNodeList) {
              let node = this.dtoToNode(http.response.result, true, uri);
              this.nodes.unshift(node);
            }
            this.selectedEntity = http.response.result;
            this.$opensilex.updateURLParameter("selected", this.selectedEntity.uri);
            this.$emit("onSelect", this.selectedEntity);
          }).catch(this.$opensilex.errorHandler);
    }
  }

  edit(uri) {
    this.getDetails(uri).then((http: HttpResponse<OpenSilexResponse>) => {
      this.$emit("onEdit", http.response.result);
    });

  }


  firstObjectOfList() {
    if (this.nodes.length > 0) {
      return this.nodes[0].data;
    }
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
</style>

<i18n>
en:
  VariableStructureList:
    variable: "(0 variables) | (1 variable) | ({count} variables)"
  EntityList:
    filter-placeholder: Search objects by label

fr:
  VariableStructureList:
    variable: "(0 variables) | (1 variable) | ({count} variables)"
  EntityList:
    filter-placeholder: Rechercher des élements par label
</i18n>