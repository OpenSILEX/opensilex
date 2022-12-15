<template>
    <opensilex-TreeView :nodes.sync="nodes" @select="displayClassDetail($event.data.uri)">
        <template v-slot:node="{ node }">
      <span class="item-icon">
        <opensilex-Icon v-if="classesParametersByURI[node.data.uri] && classesParametersByURI[node.data.uri].icon"
                        :icon="classesParametersByURI[node.data.uri].icon"/>
      </span>&nbsp;
            <strong v-if="node.data.selected">{{ node.title }}</strong>
            <span v-if="!node.data.selected">{{ node.title }}</span>
        </template>

        <template v-slot:buttons="{ node }">
            <opensilex-EditButton
                v-if="isManagedClass(node.data.uri) && user.isAdmin()"
                @click="$emit('editClass' ,node.data)"
                label="OntologyClassTreeView.edit"
                :small="true"
            ></opensilex-EditButton>
            <opensilex-AddChildButton
                v-if="user.isAdmin()"
                @click="$emit('createChildClass' ,node.data.uri)"
                label="OntologyClassTreeView.add-child"
                :small="true"
            ></opensilex-AddChildButton>
            <opensilex-DeleteButton
                v-if="isManagedClass(node.data.uri) && user.isAdmin()"
                @click="$emit('deleteRDFType' ,node.data)"
                label="OntologyClassTreeView.delete"
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
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Store} from "vuex";
import {VueJsOntologyExtensionService} from "../../lib";

@Component
export default class OntologyClassTreeView extends Vue {

    $opensilex: OpenSilexVuePlugin;
    $store: Store<any>;
    ontologyService: OntologyService;
    vueJsOntologyService: VueJsOntologyExtensionService;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    @Prop()
    rdfType;

    public nodes = [];

    public selected = null;

    created() {
        this.ontologyService = this.$opensilex.getService("opensilex-core.OntologyService");
        this.vueJsOntologyService = this.$opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
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

    @Watch("rdfType")
    onRootClassChange() {
        if (this.rdfType) {
            this.refresh(undefined, undefined);
        }
    }

    resourceTree: Array<ResourceTreeDTO> = null;

    getTree() {
        return this.resourceTree;
    }

    classesParametersByURI = {};

    refresh(selection, nameFilter) {

        Promise.all([
            this.ontologyService.searchSubClassesOf(this.rdfType, nameFilter, false),
            this.vueJsOntologyService.getRDFTypesParameters()
        ]).then(results => {
            let classesParameters = results[1].response.result;
            this.classesParametersByURI = {};
            for (let i in classesParameters) {
                this.classesParametersByURI[classesParameters[i].uri] = classesParameters[i];
            }

            if (results[0].response.result.length > 0) {
                this.resourceTree = results[0].response.result;

                // push the root class on the first tree level, and recursively build nodes for descendant
                this.nodes = [this.dtoToNode(this.resourceTree[0], selection)];

            } else {
                this.nodes = [];
            }

            if (selection) {
                this.displayClassDetail(selection.uri);
            }
        }).catch(this.$opensilex.errorHandler);
    }

    displayClassDetail(uri) {
        this.vueJsOntologyService
            .getRDFTypeProperties(uri, this.rdfType)
            .then(http => {
                this.selected = http.response.result;
                this.$emit("selectionChange", this.selected);
            }).catch(this.$opensilex.errorHandler);
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

    isManagedClass(rdfClassURI) {
        return !!this.classesParametersByURI[rdfClassURI];
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
