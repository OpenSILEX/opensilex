<template>
    <b-card>
        <!-- Card body -->
        <!-- Filter -->
        <opensilex-StringFilter
            :filter.sync="nameFilter"
            @update="updateFilter()"
            placeholder="EntityList.filter-placeholder"
        ></opensilex-StringFilter>

        <opensilex-TreeView :nodes.sync="nodes" @select="displayNodesDetail">
            <template v-slot:node="{ node }">
                <span class="item-icon">
                    <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.rdf_type)"/>
                </span>&nbsp;
                <!-- <strong v-if="node.data.selected">{{ node.title }}</strong> -->
                <strong v-if="node.data.selected">{{ node.data.variables ? node.title + ' ' + $tc('VariableStructureList.variable', node.data.variables.length, { count: node.data.variables.length }) : node.title }}</strong>
                <!-- <span v-if="!node.data.selected">{{ node.title }}</span> -->
                <span v-if="!node.data.selected">{{ node.data.variables ? node.title + ' ' + $tc('VariableStructureList.variable', node.data.variables.length, { count: node.data.variables.length }) : node.title }}</span>
            </template>

            <template v-slot:buttons="{ node }">
                <opensilex-EditButton
                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                    @click="edit(node.data.uri)"
                    label="Edit"
                    :small="true"
                ></opensilex-EditButton>
                <opensilex-DeleteButton
                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID) && type == 'VariableGroup'"
                    @click="deleteVariablesGroup(node.data.uri)"
                    label="Delete"
                    :small="true"
                ></opensilex-DeleteButton>
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
import { VariablesService, NamedResourceDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

@Component
export default class VariableStructureList extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    $route: any;
    service: VariablesService;
    $i18n: any;
    
    @PropSync("_type")
    type: string;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    private nameFilter: any = "";

    updateFilter() {
        this.$opensilex.updateURLParameter("name", this.nameFilter, "");
        this.refresh(false);
    }
    
    created() {
        this.service = this.$opensilex.getService("opensilex-core.VariablesService");

        let query: any = this.$route.query;
        if (query && query.name) {
            this.nameFilter = decodeURIComponent(query.name);
        }
        if (query && query.selected) {
            this.refresh(false,decodeURIComponent(query.selected));
        } else {
            this.refresh(false);
        }
    }

    mounted() {
        this.$store.watch(
            () => this.$store.getters.language,
            lang => {
                if (this.selected != null) {
                    this.displayNodeDetail(this.selected.uri, true);
                }
            }
        );
    }

    searchElements(nameFilter : string): Promise<HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>> {

        let orderBy = ["name=asc"];

        switch (this.type) {

            case VariablesView.ENTITY_TYPE: {
                return this.service.searchEntities(nameFilter,orderBy);
            }
            case VariablesView.INTEREST_ENTITY_TYPE: {
                return this.service.searchInterestEntity(nameFilter,orderBy);
            }
            case VariablesView.CHARACTERISTIC_TYPE: {
                return this.service.searchCharacteristics(nameFilter,orderBy);
            }
            case VariablesView.METHOD_TYPE: {
                return this.service.searchMethods(nameFilter,orderBy);
            }
            case VariablesView.UNIT_TYPE: {
                return this.service.searchUnits(nameFilter,orderBy);
            }
            case VariablesView.GROUP_VARIABLE_TYPE: {
                return this.service.searchVariablesGroups(nameFilter, undefined, orderBy);
            }
            default: { 
                return this.service.searchEntities(this.nameFilter,orderBy);
            }
        }
    }

    refresh(updateType : boolean, uri?) {

        if(this.type == VariablesView.VARIABLE_TYPE){
            return;
        }

        if(updateType){
            this.nameFilter = "";
        }

        this.searchElements(this.nameFilter)
            .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                let treeNode = [];
                let first = true;
                let uriFoundInSearch = false;
                for (let i in http.response.result) {
                    let resourceTree: NamedResourceDTO = http.response.result[i];
                    let node = this.dtoToNode(resourceTree, first, uri);
                    treeNode.push(node);

                    if(uri == null){
                        if(first){
                            first = false;
                        }
                    }else{
                        if(! uriFoundInSearch && resourceTree.uri == uri){
                            uriFoundInSearch = true;
                        }
                    }
                }

                if (uri != null) {
                    // display the node detail in head on list, only if the node is not already included into the search results
                    this.displayNodeDetail(uri, true, ! uriFoundInSearch);
                }

                if (http.response.result.length == 0) {
                    this.selected = null;
                }

                this.nodes = treeNode;
            }).catch(this.$opensilex.errorHandler);

        if(this.nodes.length > 0){
            this.selected = this.nodes[0].data;
        }
    }

    private dtoToNode(dto: NamedResourceDTO, first: boolean, uri: string) {
        let isLeaf = true;

        let childrenDTOs = [];

        let isSelected = first && uri == null;
        if (uri != null) {
            isSelected = uri == dto.uri;
        }
        return {
            title: dto.name,
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

    private selected: any;

    public displayNodesDetail(node: any) {
        this.displayNodeDetail(node.data.uri);
    }

    private getDetails(uri: string): Promise<HttpResponse<OpenSilexResponse>> {

        switch (this.type) {
            case VariablesView.ENTITY_TYPE : {
                return this.service.getEntity(uri);
            }
            case VariablesView.INTEREST_ENTITY_TYPE : {
                return this.service.getInterestEntity(uri);
            }            
            case VariablesView.CHARACTERISTIC_TYPE : {
                return this.service.getCharacteristic(uri);
            }
            case VariablesView.METHOD_TYPE: {
                return this.service.getMethod(uri);
            }
            case VariablesView.UNIT_TYPE: {
                return this.service.getUnit(uri);
            }
            case VariablesView.GROUP_VARIABLE_TYPE: {
                return this.service.getVariablesGroup(uri);
            }   
            default : {
                return this.service.getEntity(uri);
            }
        }
    }

    public displayNodeDetail(uri: string, forceRefresh?: boolean, appendNodeToNodeList?: boolean) {
        if ((forceRefresh || this.selected == null || this.selected.uri != uri) && this.type != VariablesView.VARIABLE_TYPE) {
            return this.getDetails(uri)
                .then((http: HttpResponse<OpenSilexResponse>) => {
                    if (appendNodeToNodeList) {
                        let node = this.dtoToNode(http.response.result, true, uri);
                        this.nodes.unshift(node);
                    }
                    this.selected = http.response.result;
                    this.$opensilex.updateURLParameter("selected", this.selected.uri);
                    this.$emit("onSelect", this.selected);
                }).catch(this.$opensilex.errorHandler);
        }
    }

    edit(uri) {
        if(this.type != VariablesView.VARIABLE_TYPE){
            this.getDetails(uri).then((http: HttpResponse<OpenSilexResponse>) => {
                this.$emit("onEdit", http.response.result);
            });
        }
    }

    deleteVariablesGroup(uri) {
        if(this.type == VariablesView.GROUP_VARIABLE_TYPE){
            this.service.deleteVariablesGroup(uri).then((http: HttpResponse<OpenSilexResponse>) => {
            let message = this.$i18n.t(http.response.result) + " " + this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);
              this.$emit("onDelete", http.response.result);
              if(this.nodes.length > 0){
                  this.selected = this.nodes[0].data;
                this.$emit("onDelete", this.nodes[0].data);
              }else{
                  this.selected = undefined;
              }
              // récuperer l'élement en top de liste
              // émettre un event @onDelete ou @topElement qui renvoie l'élement le plus haut
              this.refresh(true);
            });
        }
    }

    firstObjectOfList(){
        if(this.nodes.length > 0){
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
        filter-placeholder: Search objects by name

fr:
    VariableStructureList:
        variable: "(0 variables) | (1 variable) | ({count} variables)"
    EntityList:
        filter-placeholder: Rechercher des élements par nom
</i18n>

