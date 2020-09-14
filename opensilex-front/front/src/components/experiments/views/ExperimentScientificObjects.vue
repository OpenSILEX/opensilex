<template>
  <div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <div class="button-zone">
            <opensilex-ExperimentFacilitySelector
              :uri="uri"
              @facilitiesUpdated="refresh"
            ></opensilex-ExperimentFacilitySelector>
            &nbsp;
            <opensilex-OntologyCsvImporter
              :experimentUri="uri"
              @csvImported="refresh"
            ></opensilex-OntologyCsvImporter>
          </div>
          <opensilex-TreeView :nodes.sync="nodes" @select="displayScientificObjectDetails">
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.type)" />
              </span>&nbsp;
              <strong v-if="node.data.selected">{{ node.title }}</strong>
              <span v-if="!node.data.selected">{{ node.title }}</span>
              <span class="async-tree-action" v-if="node.data.childCount> 0  && node.data.childCount > node.children.length"> ({{node.children.length}}/{{node.data.childCount}} éléments<span v-if="node.data.childCount > node.children.length"> - <a href="#" @click.prevent="loadMoreChildren(node)">Load more...</a></span>)</span>
            </template>

            <!-- <template v-slot:buttons="{ node }">
              <opensilex-EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
                @click="editScientificObject(node.data.uri)"
                label="ExperimentScientificObjects.edit-scientific-objects"
                :small="true"
              ></opensilex-EditButton>
            </template> -->
          </opensilex-TreeView>
          <opensilex-ModalForm
            v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
            ref="soForm"
            component="opensilex-ScientificObjectForm"
            createTitle="component.group.add"
            editTitle="component.group.update"
            modalSize="lg"
            @onCreate="refresh()"
            @onUpdate="refresh()"
          ></opensilex-ModalForm>
        </b-card>
      </div>
      <div class="col-md-6">
        <opensilex-ScientificObjectDetail :selected="selected" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ScientificObjectsService,
  PartialResourceTreeDTO
} from "opensilex-core/index";
import HttpResponse from "opensilex-core/HttpResponse";
@Component
export default class ExperimentScientificObjects extends Vue {
  $opensilex: any;
  soService: ScientificObjectsService;
  uri: string;

  @Ref("soForm") readonly soForm!: any;
  @Ref("csvImporter") readonly csvImporter!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  public nodes = [];

  public selected = null;

  @Ref("facilitySelector") readonly facilitySelector!: any;

  created() {
    this.uri = this.$route.params.uri;

    this.soService = this.$opensilex.getService(
      "opensilex.ScientificObjectsService"
    );

    this.refresh();
  }

  refresh() {
    this.soService.getScientificObjectsTree(this.uri).then(http => {
      let treeNode = [];
      let first = true;
      for (let i in http.response.result) {
        let resourceTree: PartialResourceTreeDTO = http.response.result[i];
        let node = this.dtoToNode(resourceTree);
        treeNode.push(node);

        this.nodes = treeNode;
      }
    });
  }

  loadAllChildren(node) {

    let nodeURI = node.data.uri;

    let root = this.nodes[node.path[0]]
    for (let i = 1; i < node.path.length; i++) {
      root = root.children[node.path[i]];
    }

    this.soService.getScientificObjectsChildren(this.uri, nodeURI).then(http => {
      let childrenNodes = [];
      for (let i in http.response.result) {
        let soDTO  = http.response.result[i];

         let soNode ={
            title: soDTO.name,
            data: soDTO,
            isLeaf: [],
            children: [],
            isExpanded: true,
            isSelected: false,
            isDraggable: false,
            isSelectable: true
        };
        childrenNodes.push(soNode);

        
      }

      root.children= childrenNodes;
    });
  }

  loadMoreChildren(node) {
    let nodeURI = node.data.uri;

    let root = this.nodes[node.path[0]]
    for (let i = 1; i < node.path.length; i++) {
      root = root.children[node.path[i]];
    }

    if (root.children.length < node.data.childCount) {
      let remainingChildren = node.data.childCount - root.children.length;
      let offset = root.children.length;
      let limit = Math.min(remainingChildren, 40);
      this.soService.getScientificObjectsChildren(this.uri, nodeURI, limit, offset).then(http => {
        let childrenNodes = [];
        for (let i in http.response.result) {
          let soDTO  = http.response.result[i];

          let soNode ={
              title: soDTO.name,
              data: soDTO,
              isLeaf: !soDTO.hasChildren,
              children: [],
              isExpanded: false,
              isSelected: false,
              isDraggable: false,
              isSelectable: true
          };
          childrenNodes.push(soNode);

          
        }

        root.children = root.children.concat(childrenNodes);

      });
    }
  }

  private dtoToNode(dto: PartialResourceTreeDTO) {
    let isLeaf = dto.children.length == 0;

    let childrenDTOs = [];
    if (!isLeaf) {
      for (let i in dto.children) {
        childrenDTOs.push(this.dtoToNode(dto.children[i]));
      }
    }

    return {
      title: dto.name,
      data: dto,
      isLeaf: isLeaf,
      children: childrenDTOs,
      isExpanded: true,
      isSelected: false,
      isDraggable: false,
      isSelectable: true
    };
  }

  editScientificObject(objectURI) {
    this.soService.getScientificObjectDetail(this.uri, objectURI).then(http => {
      this.soForm.showEditForm(http.response.result);
    });
  }

  public displayScientificObjectDetails(node: any) {
    this.soService
      .getScientificObjectDetail(this.uri, node.data.uri)
      .then(http => {
        this.selected = http.response.result;
      });
  }
}
</script>

<style scoped lang="scss">
.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}
</style>

<i18n>
en:
  ExperimentScientificObjects:
    import-scientific-objects: Import experiment objets

fr:
  ExperimentScientificObjects:
    import-scientific-objects:  Importer des objets d'étude
</i18n>
