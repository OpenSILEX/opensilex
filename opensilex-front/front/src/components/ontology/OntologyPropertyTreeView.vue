<template>
    <opensilex-TreeView :nodes.sync="nodes" @select="displayPropertyNodeDetail">
        <template v-slot:node="{ node }">
      <span class="item-icon">
        <opensilex-Icon :icon="getPropertyIcon(node)"/>
      </span>&nbsp;
            <strong v-if="node.data.selected">{{ node.title }}</strong>
            <span v-if="!node.data.selected">{{ node.title }}</span>
        </template>

        <template v-slot:buttons="{ node }">
            <opensilex-EditButton
                v-if="user.isAdmin()"
                @click="$emit('editProperty' ,node.data)"
                label="OntologyPropertyTreeView.edit"
                :small="true"
            ></opensilex-EditButton>
            <opensilex-AddChildButton
                v-if="user.isAdmin()"
                @click="$emit('createChildProperty' ,node.data.uri)"
                label="OntologyPropertyTreeView.add-child"
                :small="true"
            ></opensilex-AddChildButton>
            <opensilex-DeleteButton
                v-if="user.isAdmin()"
                @click="$emit('deleteProperty' ,node.data)"
                label="OntologyPropertyTreeView.delete"
                :small="true"
            ></opensilex-DeleteButton>
        </template>
    </opensilex-TreeView>
</template>

<script lang="ts">
import {Component, Prop, Watch} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {OntologyService, ResourceTreeDTO} from "opensilex-core/index";
import OWL from "../../ontologies/OWL";
import {Route} from "vue-router";

@Component
export default class OntologyPropertyTreeView extends Vue {
    $opensilex: any;
    $store: any;
    $route: Route;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    @Prop()
    domain;

    public nodes = [];

    selected = null;

    ontologyService: OntologyService;

    created() {
        this.ontologyService = this.$opensilex.getService(
            "opensilex-core.OntologyService"
        );
      let preselected = this.$route.query.selected;
      if(typeof preselected === "string"){
        this.displayPropertyDetail(preselected, undefined);
      }
        this.onDomainChange();
    }

    getTree() {
        return this.nodes;
    }

    private langUnwatcher;

    mounted() {
        this.langUnwatcher = this.$store.watch(
            () => this.$store.getters.language,
            lang => {
                this.onDomainChange();
                if (this.selected) {
                    this.displayPropertyDetail(this.selected.uri, this.selected.rdf_type);
                }
            }
        );
    }

    beforeDestroy() {
        this.langUnwatcher();
    }

    @Watch("domain")
    onDomainChange() {
        if (this.domain) {
            this.refresh(undefined);
        }
    }

    refresh(nameFilter) {

        // get properties on domain, including those from sub-classes
        this.ontologyService.getProperties(this.domain,nameFilter,true)
            .then(http => {
                if (http && http.response.result) {
                    let treeNode = [];
                    http.response.result.forEach((resourceTree: ResourceTreeDTO) => {
                        let node = this.dtoToNode(resourceTree);
                        treeNode.push(node);
                    });
                    this.nodes = treeNode;
                } else {
                    this.nodes = [];
                }
                this.$emit("selectionChange", this.selected);
            }).catch(this.$opensilex.errorHandler);
    }

    displayPropertyNodeDetail(node) {
        if (!this.selected || node.data.uri != this.selected.uri) {
            this.displayPropertyDetail(node.data.uri, node.data.rdf_type);
        }
    }

    displayPropertyDetail(uri, type) {
        this.ontologyService.getProperty(uri, type, this.domain).then(http => {
            this.selected = http.response.result;
            //This updates or adds a url parameter, permitting refresh and navigation to specific elements
            this.$opensilex.updateURLParameter("selected", this.selected.uri);
            this.$emit("selectionChange", this.selected);
        });
    }

    getPropertyIcon(node) {
        if (OWL.isDatatypeProperty(node.data.rdf_type)) {
            return "fa#database";
        } else {
            return "fa#link";
        }
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
            isSelected: this.selected && this.selected.uri == dto.uri,
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
    OntologyPropertyTreeView:
        edit: Edit property
        add-child: Add sub-property
        delete: Delete property

fr:
    OntologyPropertyTreeView:
        edit: Editer la propriété
        add-child: Ajouter une sous-propriété
        delete: Supprimer la propriété

</i18n>
