<template>

    <div class="row">
        <div class="col col-xl-12">

            <div class="card">
                <div class="card-header">
                    <h3>
                        <i class="ik ik-clipboard"></i>
                        {{ $t('Annotation.list-title') }}
                    </h3>
                </div>

                <div class="card-body">
                    <div class="button-zone">
                        <opensilex-CreateButton
                                v-if="user.hasCredential(modificationCredentialId)"
                                label="Annotation.add"
                                @click="annotationModalForm.showCreateForm([target])"
                        ></opensilex-CreateButton>
                    </div>
                    <opensilex-PageContent 
                           v-if="renderComponent">

                        <template v-slot>

                            <opensilex-TableAsyncView
                                    ref="tableRef"
                                    :searchMethod="search"
                                    :fields="fields"
                                    :isSelectable="isSelectable"
                            >

                                <template v-slot:cell(created)="{data}">
                                    <opensilex-TextView
                                            :value="new Date(data.item.created).toLocaleString()">
                                    </opensilex-TextView>
                                </template>

                                <template v-slot:cell(author)="{data}">
                                    <opensilex-TextView v-if="data.item.author"
                                                          :value="getUserNames(data.item.author)">
                                    </opensilex-TextView>
                                </template>

                                <template v-slot:cell(description)="{data}">
                                    <opensilex-TextView v-if="data.item.description" :value="data.item.description">
                                    </opensilex-TextView>
                                </template>

                                <template v-slot:cell(motivation)="{data}">
                                    <opensilex-TextView v-if="data.item.motivation" :value="data.item.motivation.name">
                                    </opensilex-TextView>
                                </template>

                                <template v-if="displayTargetColumn" v-slot:cell(targets)="{data}">
                                    <opensilex-TextView :value="data.item.targets[0]">
                                    </opensilex-TextView>
                                </template>

                                <template v-slot:cell(uri)="{data}">
                                    <opensilex-TextView v-if="data.item.uri" :value="data.item.uri">
                                    </opensilex-TextView>
                                </template>

                                <template v-slot:cell(actions)="{data}">
                                    <b-button-group size="sm">
                                        <opensilex-EditButton
                                                v-if="! modificationCredentialId || user.hasCredential(modificationCredentialId)"
                                                @click="editAnnotation(data.item)"
                                                label="Annotation.edit"
                                                :small="true"
                                        ></opensilex-EditButton>
                                        <opensilex-DeleteButton
                                                v-if="! deleteCredentialId || user.hasCredential(deleteCredentialId)"
                                                @click="deleteAnnotation(data.item.uri)"
                                                label="Annotation.delete"
                                                :small="true"
                                        ></opensilex-DeleteButton>
                                    </b-button-group>
                                </template>
                            </opensilex-TableAsyncView>
                        </template>
                    </opensilex-PageContent>

                    <opensilex-AnnotationModalForm

                            ref="annotationModalForm"
                            @onCreate="refresh"
                            @onUpdate="refresh"
                    ></opensilex-AnnotationModalForm>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts">
    import {Component, Ref, Prop, Watch} from "vue-property-decorator";
    import Vue from "vue";

    import {AnnotationsService} from "opensilex-core/api/annotations.service";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
    import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";
    import AnnotationModalForm from "../form/AnnotationModalForm.vue";
    import {UserGetDTO} from "opensilex-security/model/userGetDTO";
    import {SecurityService} from "opensilex-security/api/security.service";

    @Component
    export default class AnnotationList extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $service: AnnotationsService
        $securityService: SecurityService;

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

        @Prop({default: AnnotationList.getDefaultColumns})
        columnsToDisplay: Set<string>;

        usersByUri: Map<string,UserGetDTO>;
        renderComponent = true;

        @Ref("tableRef") readonly tableRef!: any;
        @Ref("annotationModalForm") readonly annotationModalForm!: AnnotationModalForm;


        static getDefaultColumns(){
            return new Set(["created","description","author","motivation","uri"]);
        }

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        refresh() {
            this.tableRef.refresh();
        }

        @Prop({default: AnnotationList.newFilter})
        filter;

        @Prop()
        target;

        @Watch("target")
        gogogo() {
            this.renderComponent = false;

            this.$nextTick(() => {
            // Add the component back in
            this.renderComponent = true;
            });
        }

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
            this.$securityService = this.$opensilex.getService("opensilex.SecurityService")
        }

        search(options) {

            return new Promise((resolve, reject) => {
                this.$service.searchAnnotations(
                    this.filter.bodyValue,
                    this.target,
                    this.filter.motivation,
                    this.filter.author,
                    options.orderBy,
                    options.currentPage,
                    options.pageSize
                ).then((http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {

                    let results = http.response.result;
                    if(results.length == 0){
                        resolve(http);
                    }else{
                        let usersPromise = this.buildUsersIndexPromise(http.response.result,reject);
                        Promise.resolve(usersPromise).then(() => {
                            resolve(http);
                        })
                    }

                }).catch(reject);

            });
        }

        buildUsersIndexPromise(annotations : Array<AnnotationGetDTO>, reject): Promise<void | HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>> {

            this.usersByUri = new Map();

            let uniqueUsers = new Set<string>();
            annotations.forEach(annotation => {
                uniqueUsers.add(annotation.author);
            });

            return this.$securityService.getUsersByURI(Array.from(uniqueUsers)).then(http => {
                http.response.result.forEach(userDto => {
                    this.usersByUri.set(userDto.uri, {first_name : userDto.first_name, last_name : userDto.last_name});
                })
            }).catch(reject);
        }

        getUserNames(userUri: string) : string{

            if(! userUri){
                return undefined;
            }
            let userDto = this.usersByUri.get(userUri);
            return userDto ? userDto.first_name + " "+userDto.last_name : undefined;
        }

        static newFilter() {
            return {
                bodyValue: undefined,
                motivation: undefined,
                created: undefined,
                author: undefined
            };
        }

        editAnnotation(annotation){
            let copy = JSON.parse(JSON.stringify(annotation));
            this.annotationModalForm.showEditForm(copy);
        }

        get fields() {

            let tableFields = [];

            if(this.columnsToDisplay.has("created")){
                tableFields.push({key: "created", label: "Annotation.created", sortable: true});
            }
            if(this.columnsToDisplay.has("author")){
                tableFields.push({key: "author", label: "Annotation.author", sortable: true});
            }
            if(this.columnsToDisplay.has("description")){
                tableFields.push({key: "description", label: "Annotation.description", sortable: true});
            }

            if(this.columnsToDisplay.has("motivation")){
                tableFields.push({key: "motivation", label: "Annotation.motivation", sortable: true});
            }
            if(this.columnsToDisplay.has("uri")){
                tableFields.push({key: "uri", label: "component.common.uri", sortable: true});
            }
            if(this.columnsToDisplay.has("targets")){
                tableFields.push({key: "targets", label: "Annotation.targets", sortable: true});
            }

            if (this.enableActions) {
                tableFields.push({key: "actions", label: "component.common.actions", sortable: false});
            }

            return tableFields;
        }

        deleteAnnotation(uri: string) {
            this.$service.deleteAnnotation(uri).then(() => {
                this.$nextTick(() => {
                    this.refresh();
                });
                let message = this.$i18n.t("Annotation.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
                this.$opensilex.showSuccessToast(message);
                this.$emit("onDelete", uri);
            }).catch(this.$opensilex.errorHandler);
        }

    }
</script>


<i18n>
en:
    Annotation:
        name: The annotation
        add: Add annotation
        edit: Edit annotation
        delete: Delete annotation
        motivation: Motivation
        motivation-placeholder: Select a motivation
        motivation-help: Intent or motivation for the creation of the Annotation.
        description: Description
        author: Author
        created: Created
        target: Target
        list-title: Annotations
        already-exist: the annotation already exist
fr:
    Annotation:
        name: L'annotation
        add: Ajouter une annotation
        edit: Éditer l'annotation
        delete: Supprimer l'annotation
        motivation: Motivation
        motivation-placeholder: Sélectionnez une motivation
        motivation-help: "Intention ou motivation guidant la création de l'annotation"
        description: Description
        created: Créée le
        author: Auteur
        target: Cible
        list-title: Annotations
        already-exist: l'annotation existe déjà
</i18n>
