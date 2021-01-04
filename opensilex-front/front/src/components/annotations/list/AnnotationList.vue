<template>
    <div>
        <opensilex-PageContent>
            <template v-slot>
        <!--        <opensilex-SearchFilterField-->
        <!--                @search="refresh()"-->
        <!--                @clear="reset()"-->
        <!--                label="component.experiment.search.label"-->
        <!--                :showAdvancedSearch="true"-->
        <!--        >-->
        <!--            <template v-slot:filters>-->
        <!--                &lt;!&ndash; Label &ndash;&gt;-->
        <!--                <opensilex-FilterField>-->
        <!--                    <opensilex-InputForm-->
        <!--                            :value.sync="filter.label"-->
        <!--                            label="ExperimentList.filter-label"-->
        <!--                            type="text"-->
        <!--                            placeholder="ExperimentList.filter-label-placeholder"-->
        <!--                    ></opensilex-InputForm>-->
        <!--                </opensilex-FilterField>-->
        <!--                -->
        <!--                <opensilex-FilterField>-->
        <!--                    <label>{{$t('ExperimentList.filter-year')}}</label>-->
        <!--                    <opensilex-StringFilter-->
        <!--                            placeholder="ExperimentList.filter-year-placeholder"-->
        <!--                            :filter.sync="filter.yearFilter"-->
        <!--                            type="number"-->
        <!--                            min="1000"-->
        <!--                            max="9999"-->
        <!--                    ></opensilex-StringFilter>-->
        <!--                </opensilex-FilterField>-->
        <!--            </template>-->

        <!--        </opensilex-SearchFilterField>-->


        <opensilex-TableAsyncView
                ref="tableRef"
                :searchMethod="search"
                :fields="fields"
                :isSelectable="isSelectable"
        >
            <template v-slot:cell(uri)="{data}">
                <opensilex-UriLink
                        :uri="data.item.uri"
                        :value="data.item.uri"
                        :to="{path: '/annotation/details/'+ encodeURIComponent(data.item.uri)}"
                ></opensilex-UriLink>
            </template>

            <template v-slot:cell(motivation)="{data}">
                {{ data.item.motivationName}}
            </template>

            <template v-if="displayTargetColumn" v-slot:cell(targets)="{data}">
                {{ data.item.targets[0]}}
            </template>

            <template v-slot:cell(bodyValue)="{data}">
                <opensilex-StringView :value="data.item.bodyValue"></opensilex-StringView>
            </template>

            <template v-slot:cell(creator)="{data}">
                <opensilex-StringView :value="data.item.creator"></opensilex-StringView>
            </template>

            <template v-slot:cell(created)="{data}">
                <opensilex-StringView :value="new Date(data.item.created).toLocaleDateString()"></opensilex-StringView>
            </template>

            <template v-slot:cell(actions)="{data}">
                <b-button-group size="sm">
                    <opensilex-EditButton
                            v-if="! modificationCredentialId || user.hasCredential(modificationCredentialId)"
                            @click="$emit('onEdit', data.item)"
                            label="AnnotationForm.edit"
                            :small="true"
                    ></opensilex-EditButton>
                    <opensilex-DeleteButton
                            v-if="! deleteCredentialId || user.hasCredential(deleteCredentialId)"
                            @click="deleteAnnotation(data.item.uri)"
                            label="AnnotationForm.delete"
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
    import {Component, Ref, Prop} from "vue-property-decorator";
    import Vue from "vue";

    import {AnnotationsService} from "opensilex-core/api/annotations.service";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
    import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";

    @Component
    export default class AnnotationList extends Vue {
        $opensilex: OpenSilexVuePlugin;
        $service: AnnotationsService
        $i18n: any;
        $store: any;

        @Prop({
            default: false
        })
        isSelectable;

        @Prop()
        modificationCredentialId;

        @Prop()
        deleteCredentialId;

        @Prop({default: true})
        enableActions;

        @Prop({default: true})
        displayTargetColumn: boolean;

        @Prop({default: new Set(["uri","motivation","bodyValue","creator","created"])})
        columnsToDisplay: Set<string>;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        @Ref("tableRef") readonly tableRef!: any;

        refresh() {
            this.tableRef.refresh();
        }

        @Prop({default: AnnotationList.newFilter})
        filter;

        @Prop()
        target;

        private langUnwatcher;
        mounted() {
            this.langUnwatcher = this.$store.watch(
                () => this.$store.getters.language,
                () => {
                    this.refresh();
                }
            );
        }

        reset() {
            this.filter = AnnotationList.newFilter();
            this.refresh();
        }


        created() {
            this.$service = this.$opensilex.getService("opensilex.AnnotationsService")
        }

        search(options) : Promise<HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>> {
            return this.$service
                .searchAnnotations(
                    this.filter.bodyValue,
                    this.target,
                    this.filter.motivation,
                    undefined,
                    options.orderBy,
                    options.currentPage,
                    options.pageSize
                );
        }

        static newFilter() {
            return {
                bodyValue: undefined,
                motivation: undefined,
                created: undefined
            };
        }

        get fields() {

            let tableFields = [];

            if(this.columnsToDisplay.has("uri")){
                tableFields.push({key: "uri", label: "component.common.uri", sortable: true});
            }
            if(this.columnsToDisplay.has("motivation")){
                tableFields.push({key: "motivation", label: "Annotation.motivation", sortable: true});
            }
            if(this.columnsToDisplay.has("targets")){
                tableFields.push({key: "targets", label: "Annotation.targets", sortable: true});
            }
            if(this.columnsToDisplay.has("bodyValue")){
                tableFields.push({key: "bodyValue", label: "Annotation.body-value", sortable: true});
            }
            if(this.columnsToDisplay.has("creator")){
                tableFields.push({key: "creator", label: "Annotation.creator", sortable: true});
            }
            if(this.columnsToDisplay.has("created")){
                tableFields.push({key: "created", label: "Annotation.created", sortable: true});
            }
            if (this.enableActions) {
                tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
            }

            return tableFields;
        }

        editAnnotation(annotation) {
            // this.create.showEditForm(annotation);
        }

        deleteAnnotation(uri: string) {
            this.$service.deleteAnnotation(uri).then(() => {
                this.$nextTick(() => {
                    this.refresh();
                });
                this.$emit("onDelete", uri);
            }).catch(this.$opensilex.errorHandler);
        }

    }
</script>


<style scoped lang="scss">
</style>
