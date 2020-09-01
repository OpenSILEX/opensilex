<template>
  <opensilex-TreeView :nodes.sync="nodes" @select="displayClassDetail($event.data.uri)">
    <template v-slot:node="{ node }">
      <span class="item-icon">
        <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.uri)" />
      </span>&nbsp;
      <strong v-if="node.data.selected">{{ node.title }}</strong>
      <span v-if="!node.data.selected">{{ node.title }}</span>
    </template>

        <template v-slot:buttons="{ node }">
      <opensilex-EditButton
        @click="$emit('editClass' ,node.data)"
        label="OntologyClassTreeView.edit"
        :small="true"
      ></opensilex-EditButton>
      <opensilex-AddChildButton
        @click="$emit('createChildClass' ,node.data.uri)"
        label="OntologyClassTreeView.add-child"
        :small="true"
      ></opensilex-AddChildButton>
      <opensilex-DeleteButton
        @click="$emit('deleteClass' ,node.data)"
        label="OntologyClassTreeView.delete"
        :small="true"
      ></opensilex-DeleteButton>
    </template>
  </opensilex-TreeView>
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

  created() {
    this.onRootClassChange();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
          if (this.selected) {
            this.displayClassDetail(this.selected.uri);
          }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  @Watch("rdfClass")
  onRootClassChange() {
    if (this.rdfClass) {
      this.refresh();
    }
  }

  resourceTree: Array<ResourceTreeDTO> = null;

  getTree() {
    return this.resourceTree;
  }

  refresh(selection?) {
    this.$opensilex.getService(
      "opensilex-core.OntologyService"
    ).getSubClassesOf(this.rdfClass, false).then(http => {
      let treeNode = [];
      let first = true;
      this.resourceTree = http.response.result;
      for (let i in this.resourceTree[0].children ) {
        let node = this.dtoToNode(this.resourceTree[0].children[i], selection);
        treeNode.push(node);

        this.nodes = treeNode;
      }

      if (selection) {
        this.displayClassDetail(selection.uri);
      }
    });
  }

  displayClassDetail(uri) {
    this.$opensilex.getService(
      "opensilex-core.VueJsOntologyExtensionService"
    ).getClassProperties(uri).then(http => {
      this.selected = http.response.result;
      this.$emit("selectionChange", this.selected);
    });
  }
  

  private dtoToNode(dto: ResourceTreeDTO, selection) {
    let isLeaf = dto.children.length == 0;

    let childrenDTOs = [];
    if (!isLeaf) {
      for (let i in dto.children) {
        childrenDTOs.push(this.dtoToNode(dto.children[i], selection));
      }
    }

    if (selection && selection.uri == dto.uri) {
      this.selected = selection;
    }

    let isSelected = this.selected && this.selected.uri == dto.uri;

    return {
      title: dto.name,
      data: dto,
      isLeaf: isLeaf,
      children: childrenDTOs,
      isExpanded: true,
      isSelected: isSelected,
      isDraggable: false,
      isSelectable: !dto.disabled
    };
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  OntologyClassTreeView:
    edit: Edit object type
    add-child: Add sub-object type
    delete: Delete object type

fr:
  OntologyClassTreeView:
    edit: Editer le type d'objet
    add-child: Ajouter un sous-type d'objet
    delete: Supprimer le type d'objet
</i18n>
