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
          <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.type)"/>
        </span>&nbsp;
                <strong v-if="node.data.selected">{{ node.title }}</strong>
                <span v-if="!node.data.selected">{{ node.title }}</span>
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
import UnitCreate from "../form/UnitCreate.vue";
import EntityCreate from "../form/EntityCreate.vue";
import VariableView from "../VariableView.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import { VariablesService, NamedResourceDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

@Component
export default class EntityList extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    $route: any;
    service: VariablesService;

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
        this.refresh();
    }

    created() {
        this.service = this.$opensilex.getService("opensilex-core.VariablesService");

        let query: any = this.$route.query;
        if (query && query.name) {
            this.nameFilter = decodeURIComponent(query.name);
        }
        if (query && query.selected) {
            this.refresh(decodeURIComponent(query.selected));
        } else {
            this.refresh();
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

    searchElements(): Promise<HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>> {

        switch (this.type) {
            // case VariableView.VARIABLE_TYPE: {
            //     return this.service.searchVariables(this.nameFilter,["name=asc"]);
            // }
            case VariableView.ENTITY_TYPE: {
                return this.service.searchEntities(this.nameFilter,["name=asc"]);
            }
            case VariableView.QUALITY_TYPE: {
                return this.service.searchQualities(this.nameFilter,["name=asc"]);
            }
            case VariableView.METHOD_TYPE: {
                return this.service.searchMethods(this.nameFilter,["name=asc"]);
            }
            case VariableView.UNIT_TYPE: {
                return this.service.searchUnits(this.nameFilter,["name=asc"]);
            }
            default: {
                return this.service.searchEntities(this.nameFilter,["name=asc"]);
            }
        }
    }

    refresh(uri?) {

        if(this.type == VariableView.VARIABLE_TYPE){
            return;
        }

        this.searchElements()
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
                            this.displayNodeDetail(node.data.uri, true,false);
                            first = false;
                        }
                    }else{
                        if(! uriFoundInSearch && resourceTree.uri == uri){
                            uriFoundInSearch = true;
                        }
                    }
                }

                if (uri != null) {
                    this.displayNodeDetail(uri, true, ! uriFoundInSearch);
                }

                if (http.response.result.length == 0) {
                    this.selected = null;
                }

                this.nodes = treeNode;
            }).catch(this.$opensilex.errorHandler);
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
            case VariableView.ENTITY_TYPE : {
                return this.service.getEntity(uri);
            }
            case VariableView.QUALITY_TYPE : {
                return this.service.getQuality(uri);
            }
            case VariableView.METHOD_TYPE: {
                return this.service.getMethod(uri);
            }
            case VariableView.UNIT_TYPE: {
                return this.service.getUnit(uri);
            }
            default : {
                return this.service.getEntity(uri);
            }
        }
    }

    public displayNodeDetail(uri: string, forceRefresh?: boolean, appendNodeToNodeList?: boolean) {
        if ((forceRefresh || this.selected == null || this.selected.uri != uri) && this.type != VariableView.VARIABLE_TYPE) {
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
        if(this.type != VariableView.VARIABLE_TYPE){
            if(this.selected){
                this.$emit("onEdit",this.selected);
            }else{
                this.getDetails(uri).then((http: HttpResponse<OpenSilexResponse>) => {
                    this.$emit("onEdit", http.response.result);
                    // this.getForm().showEditForm(http.response.result);
                });
            }
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
    EntityList:
        filter-placeholder: Search objects by name

fr:
    EntityList:
        filter-placeholder: Rechercher des élements par nom
</i18n>

