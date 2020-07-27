<template>
  <div>
    <div class="row">
      <div class="col-md-6">
        <b-card>
          <opensilex-TreeView :nodes.sync="nodes" @select="displayClassDetail">
            <template v-slot:node="{ node }">
              <span class="item-icon">
                <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.uri)" />
              </span>&nbsp;
              <strong v-if="node.data.selected">{{ node.title }}</strong>
              <span v-if="!node.data.selected">{{ node.title }}</span>
            </template>

            <!-- <template v-slot:buttons="{ node }">
              <opensilex-EditButton
                @click="editOntologyType(node.data.uri)"
                label="OntologyClassTreeView.edit"
                :small="true"
              ></opensilex-EditButton>
            </template>-->
          </opensilex-TreeView>
        </b-card>
      </div>
      <div class="col-md-6">
        <opensilex-OntologyClassDetail :selected="selected" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop, Watch } from "vue-property-decorator";
import Vue from "vue";
import { OntologyService, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse from "opensilex-core/HttpResponse";
@Component
export default class OntologyClassTreeView extends Vue {
  $opensilex: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  rdfClass;

  public nodes = [];

  public selected = null;

  ontologyService: OntologyService;

  created() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );

    this.onRootClassChange();
  }

  @Watch("rdfClass")
  onRootClassChange() {
    console.error("ERDF", this.rdfClass)
    if (this.rdfClass) {
      this.refresh();
    }
  }

  refresh() {
    this.ontologyService.getSubClassesOf(this.rdfClass, true).then(http => {
      let treeNode = [];
      let first = true;
      for (let i in http.response.result) {
        let resourceTree: ResourceTreeDTO = http.response.result[i];
        let node = this.dtoToNode(resourceTree);
        treeNode.push(node);

        this.nodes = treeNode;
      }
    });
  }

  displayClassDetail(node) {
    this.ontologyService.getClass(node.data.uri).then(http => {
      this.selected = http.response.result;
    });
  }

  private dtoToNode(dto: ResourceTreeDTO) {
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
      isSelected: dto,
      isDraggable: false,
      isSelectable: !dto.disabled
    };
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
</i18n>
