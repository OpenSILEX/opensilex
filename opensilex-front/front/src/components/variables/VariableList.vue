<template>
    <div class="container-fluid">
        <opensilex-PageHeader
                icon="ik#ik-layers"
                title="component.menu.variables"
                description="component.variable.description"
        ></opensilex-PageHeader>

        <opensilex-PageActions
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
        >
            <template v-slot>
                <opensilex-CreateButton
                        @click="variableForm.showCreateForm()"
                        label="component.variable.form.add.variable"></opensilex-CreateButton>
            </template>
        </opensilex-PageActions>

        <opensilex-SearchFilterField :withButton="false">
            <template v-slot:filters>
                <div class="col col-xl-4 col-sm-6 col-12">
                    <label>{{$t('component.project.filter-label')}}:</label>
                    <opensilex-StringFilter
                            :filter.sync="labelFilter"
                            @update="updateLabelFilter()"
                            placeholder="component.project.filter-label-placeholder"
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

                    <template v-slot:cell(label)="{data}">{{data.item.label}}</template>
                    <template v-slot:cell(longname)="{data}">{{data.item.longName}}</template>
                    <template v-slot:cell(entity)="{data}">{{getEntityName(data.item)}}</template>
                    <template v-slot:cell(quality)="{data}">{{getQualityName(data.item)}}</template>
                    <template v-slot:cell(method)="{data}">{{getMethodName(data.item)}}</template>
                    <template v-slot:cell(unit)="{data}">{{getUnitName(data.item)}}</template>

                    <template v-slot:cell(actions)="{data}">
                        <b-button-group size="sm">
                            <!--                            <opensilex-EditButton-->
                            <!--                                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"-->
                            <!--                                    @click="showEditForm(data.item.uri)"-->
                            <!--                                    label="component.experiment.update"-->
                            <!--                                    :small="true"-->
                            <!--                            ></opensilex-EditButton>-->
                            <opensilex-DeleteButton
                                    v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID)"
                                    @click="deleteVariable(data.item.uri)"
                                    label="component.experiment.delete"
                                    :small="true"
                            ></opensilex-DeleteButton>
                        </b-button-group>
                    </template>
                </opensilex-TableAsyncView>
            </template>
        </opensilex-PageContent>

        <opensilex-VariableForm
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                ref="variableForm"
                @onCreate="refresh()"
                @onUpdate="refresh()"
        ></opensilex-VariableForm>
    </div>
</template>

<script lang="ts">
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {
        VariablesService,
        VariableGetDTO
    } from "opensilex-core/index";
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";

    @Component
    export default class VariableList extends Vue {
        $opensilex: any;
        $service: VariablesService;

        @Ref("variableForm") readonly variableForm!: any;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        private labelFilter: any = "";

        updateLabelFilter() {
            this.$opensilex.updateURLParameter("label", this.labelFilter, "");
            this.refresh();
        }

        @Ref("tableRef") readonly tableRef!: any;

        refresh() {
            this.tableRef.refresh();
        }

        searchVariables(options) {

            return this.$service
                .searchVariables(
                    this.labelFilter,
                    options.orderBy,
                    options.currentPage,
                    options.pageSize
                );
        }

        created() {
            let query: any = this.$route.query;
            if (query.label) {
                this.labelFilter = decodeURI(query.label);
            }
            this.$opensilex.disableLoader();
            this.$service = this.$opensilex.getService("opensilex.VariablesService");
        }

        getEntityName(dto: VariableGetDTO): String {
            return dto.entity ? dto.entity.name : null;
        }

        getQualityName(dto: VariableGetDTO): String {
            return dto.quality ? dto.quality.name : null;
        }

        getMethodName(dto: VariableGetDTO): String {
            return dto.method ? dto.method.name : null;
        }

        getUnitName(dto: VariableGetDTO): String {
            return dto.unit ? dto.unit.name : null;
        }

        fields = [
            {
                key: "uri",
                label: "component.common.uri",
                sortable: true
            },
            {
                key: "label",
                label: "component.common.name",
                sortable: true
            },
            {
                key: "longname",
                label: "component.variable.longname",
                sortable: true
            },
            {
                key: "entity",
                label: "component.variable.entity",
                sortable: true
            },
            {
                key: "quality",
                label: "component.variable.quality",
                sortable: true
            },
            {
                key: "method",
                label: "component.variable.method",
                sortable: true
            },
            {
                key: "unit",
                label: "component.variable.unit",
                sortable: true
            },
            {
                label: "component.common.actions",
                key: "actions"
            }
        ];

        // showEditForm(uri: string) {
        //     this.$opensilex
        //         .getService("opensilex.ExperimentsService")
        //         .getExperiment(uri)
        //         .then(http => {
        //             this.experimentForm.showEditForm(http.response.result);
        //         });
        // }

        deleteVariable(uri: string) {
            this.$service.deleteVariable(uri)
                .then(() => {
                    this.refresh();
                })
                .catch(this.$opensilex.errorHandler);
        }
    }
</script>


<style scoped lang="scss">
</style>