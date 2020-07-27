<template>
    <div class="container-fluid">

        <opensilex-SearchFilterField :withButton="false">
            <template v-slot:filters>
                <div class="col col-xl-4 col-sm-6 col-12">
                    <label>{{$t('Variables.label-filter')}}:</label>
                    <opensilex-StringFilter
                            :filter.sync="nameFilter"
                            @update="updateLabelFilter()"
                            placeholder="Variables.label-filter-placeholder"
                    ></opensilex-StringFilter>
                </div>

            </template>
        </opensilex-SearchFilterField>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-TableAsyncView ref="tableRef" :searchMethod="searchVariables" :fields="fields">
                    <template v-slot:cell(uri)="{data}">
                        <opensilex-UriLink
                                :uri="data.item.uri"
                                :to="{path: '/variable/'+ encodeURIComponent(data.item.uri)}"
                        ></opensilex-UriLink>
                    </template>

                    <template v-slot:cell(name)="{data}">{{data.item.name}}</template>
                    <template v-slot:cell(entity)="{data}">{{data.item.entity.name}}</template>
                    <template v-slot:cell(qualityLabel)="{data}">{{data.item.quality.name}}</template>
                    <template v-slot:cell(methodLabel)="{data}">{{data.item.method.name}}</template>
                    <template v-slot:cell(unitLabel)="{data}">{{data.item.unit.name}}</template>

                    <template v-slot:cell(actions)="{data}">
                        <b-button-group size="sm">
                            <opensilex-EditButton
                                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                                    @click="$emit('onEdit', data.item.uri)"
                                    label="Variables.edit"
                                    :small="true"
                            ></opensilex-EditButton>
                            <opensilex-InteroperabilityButton
                                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                                    :small="true"
                                    label="component.skos.manage"
                                    @click="$emit('onInteroperability', data.item.uri)"
                            ></opensilex-InteroperabilityButton>
                            <opensilex-DeleteButton
                                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID)"
                                    @click="$emit('onDelete', data.item.uri)"
                                    label="Variables.delete"
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
    import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

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

        updateLabelFilter() {
            this.$opensilex.updateURLParameter("name", this.nameFilter, "");
            this.refresh();
        }

        @Ref("tableRef") readonly tableRef!: any;

        refresh() {
            this.tableRef.refresh();
        }

        searchVariables(options) {
            return this.$service
                .searchVariables(
                    this.nameFilter,
                    options.orderBy,
                    options.currentPage,
                    options.pageSize
                );
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
                key: "uri",
                label: "component.common.uri",
                sortable: true
            },
            {
                key: "name",
                label: "component.common.name",
                sortable: true
            },
            {
                key: "entity",
                label: "Variables.entity",
                sortable: true
            },
            {
                key: "qualityLabel",
                label: "Variables.quality",
                sortable: true
            },
            {
                key: "methodLabel",
                label: "Variables.method",
                sortable: true
            },
            {
                key: "unitLabel",
                label: "Variables.unit",
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