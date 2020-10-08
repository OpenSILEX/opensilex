<template>
    <div>
        <opensilex-StringFilter
            :filter.sync="nameFilter"
            @update="updateFilters()"
            placeholder="VariableList.label-filter-placeholder"
        ></opensilex-StringFilter>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchVariables" :fields="fields"
                                          defaultSortBy="name">

                    <template v-slot:cell(name)="{data}">
                        <opensilex-UriLink
                            :uri="data.item.uri"
                            :value="data.item.name"
                            :to="{path: '/variable/'+ encodeURIComponent(data.item.uri)}"
                        ></opensilex-UriLink>
                    </template>
                    <template v-slot:cell(_entity_name)="{data}">{{ data.item.entity.name }}</template>
                    <template v-slot:cell(_quality_name)="{data}">{{ data.item.quality.name }}</template>
                    <template v-slot:cell(_method_name)="{data}">{{ data.item.method ? data.item.method.name : ""  }}</template>
                    <template v-slot:cell(_unit_name)="{data}">{{ data.item.unit ? data.item.unit.name : "" }}</template>

                    <template v-slot:cell(actions)="{data}">
                        <b-button-group size="sm">
                            <opensilex-EditButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                                @click="$emit('onEdit', data.item.uri)"
                                label="component.common.list.buttons.update"
                                :small="true"
                            ></opensilex-EditButton>
                            <opensilex-InteroperabilityButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                                :small="true"
                                label="component.common.list.buttons.interoperability"
                                @click="$emit('onInteroperability', data.item.uri)"
                            ></opensilex-InteroperabilityButton>
                            <opensilex-DeleteButton
                                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID)"
                                @click="$emit('onDelete', data.item.uri)"
                                label="component.common.list.buttons.delete"
                                :small="true"
                            ></opensilex-DeleteButton>
                        </b-button-group>
                    </template>
                </opensilex-TableAsyncView>
            </template>
        </opensilex-PageContent>

    </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {
    VariablesService
} from "opensilex-core/index";

@Component
export default class VariableList extends Vue {
    $opensilex: any;
    $service: VariablesService;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    private nameFilter: any = "";

    updateFilters() {
        this.$opensilex.updateURLParameter("name", this.nameFilter, "");
        this.refresh();
    }

    resetSearch() {
        this.nameFilter = "";
        this.$opensilex.updateURLParameter("name",undefined,undefined);
        this.refresh();
    }


    @Ref("tableRef") readonly tableRef!: any;

    refresh() {
        this.tableRef.refresh();
    }

    searchVariables(options) {
        return this.$service.searchVariables(this.nameFilter, options.orderBy, options.currentPage, options.pageSize);
    }

    created() {
        let query: any = this.$route.query;
        if (query.name) {
            this.nameFilter = decodeURI(query.name);
        }
        this.$opensilex.disableLoader();
        this.$service = this.$opensilex.getService("opensilex.VariablesService");
    }

    fields = [
        {
            key: "name",
            label: "component.common.name",
            sortable: true
        },
        {
            key: "_entity_name",
            label: "VariableView.entity",
            sortable: true
        },
        {
            key: "_quality_name",
            label: "VariableView.quality",
            sortable: true
        },
        {
            key: "_method_name",
            label: "VariableView.method",
            sortable: true
        },
        {
            key: "_unit_name",
            label: "VariableView.unit",
            sortable: true
        },
        {
            label: "component.common.actions",
            key: "actions"
        }
    ];

}
</script>


<style scoped lang="scss">
</style>


<i18n>

en:
    VariableList:
        label-filter: Search variables
        label-filter-placeholder: "Search variables, plant height, plant, humidity, image processing, percentage, air.*humidity, etc.
            This filter apply on URI, name, alternative name, or on entity/characteristic/method/unit name."
fr:
    VariableList:
        label-filter: Chercher une variable
        label-filter-placeholder: "Rechercher des variables : Hauteur de plante, plante, humidité, analyse d'image, pourcentage, air.*humidité, etc.
            Ce filtre s'applique à l'URI, au nom, au nom alternatif et au nom de l'entité/caractéristique/méthode/unité."

</i18n>