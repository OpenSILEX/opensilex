<template>
    <div class="container-fluid" v-if="germplasm.uri">
        <opensilex-PageHeader
                icon="fa#sun"
                :title="germplasm.name"
                description="GermplasmDetails.title"
        ></opensilex-PageHeader>

        <opensilex-PageActions :tabs=true :returnButton="true">
            <b-nav-item
                    :active="isDetailsTab()"
                    :to="{path: '/germplasm/details/' + encodeURIComponent(uri)}"
            >{{ $t('component.common.details-label') }}
            </b-nav-item>

            <b-nav-item
                    class="ml-3"
                    :active="isAnnotationTab()"
                    :to="{ path: '/germplasm/annotations/' + encodeURIComponent(uri) }"
            >{{ $t("Annotation.list-title") }}
            </b-nav-item>

            <opensilex-Button
                    v-if="isAnnotationTab() && user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
                    label="Annotation.add" variant="primary" :small="false" icon="fa#edit"
                    @click="annotationModalForm.showCreateForm()"
            ></opensilex-Button>

            <opensilex-AnnotationModalForm
                    v-if="isAnnotationTab() && user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
                    ref="annotationModalForm"
                    :target="uri"
                    @onCreate="updateAnnotations"
                    @onUpdate="updateAnnotations"
            ></opensilex-AnnotationModalForm>

        </opensilex-PageActions>

        <opensilex-PageContent>

            <template v-slot>
                <b-row v-if="isDetailsTab()">
                    <b-col>
                        <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
                            <template v-slot:rightHeader>
                                <opensilex-EditButton
                                        v-if="!germplasm.type.endsWith('Species')"
                                        @click="updateGermplasm"
                                ></opensilex-EditButton>
                                <opensilex-DeleteButton
                                        @click="deleteGermplasm"
                                ></opensilex-DeleteButton>
                            </template>
                            <template v-slot:body>
                                <opensilex-UriView
                                        v-if="germplasm.uri.startsWith('http')"
                                        :uri="germplasm.uri"
                                        :url="germplasm.uri"
                                ></opensilex-UriView>
                                <opensilex-StringView class="test" v-else label="GermplasmDetails.uri"
                                                      :value="germplasm.uri"></opensilex-StringView>
                                <opensilex-StringView label="GermplasmDetails.rdfType"
                                                      :value="germplasm.typeLabel"></opensilex-StringView>
                                <opensilex-StringView label="GermplasmDetails.name"
                                                      :value="germplasm.name"></opensilex-StringView>
                                <opensilex-StringView
                                        v-if="germplasm.synonyms.length>0"
                                        label="GermplasmDetails.synonyms"
                                        :value="germplasm.synonyms.toString()"
                                ></opensilex-StringView>
                                <opensilex-StringView
                                        v-if="germplasm.code != null"
                                        label="GermplasmDetails.code"
                                        :value="germplasm.code"
                                ></opensilex-StringView>
                                <opensilex-StringView
                                        v-if="germplasm.institute != null"
                                        label="GermplasmDetails.institute"
                                        :value="germplasm.institute"
                                ></opensilex-StringView>
                                <opensilex-StringView
                                        v-if="germplasm.productionYear != null"
                                        label="GermplasmDetails.year"
                                        :value="germplasm.productionYear"
                                ></opensilex-StringView>
                                <opensilex-StringView
                                        v-if="germplasm.comment != null"
                                        label="GermplasmDetails.comment"
                                        :value="germplasm.comment"
                                ></opensilex-StringView>
                                <opensilex-LabelUriView
                                        v-if="(germplasm.speciesLabel != null) || (germplasm.species != null)"
                                        label="GermplasmDetails.species"
                                        :value="germplasm.speciesLabel"
                                        :uri="germplasm.species"
                                ></opensilex-LabelUriView>
                                <opensilex-LabelUriView
                                        v-if="(germplasm.varietyLabel != null) || (germplasm.variety != null)"
                                        label="GermplasmDetails.variety"
                                        :value="germplasm.varietyLabel"
                                        :uri="germplasm.variety"
                                ></opensilex-LabelUriView>
                                <opensilex-LabelUriView
                                        v-if="(germplasm.accessionLabel != null) || (germplasm.accession != null)"
                                        label="GermplasmDetails.accession"
                                        :value="germplasm.accessionLabel"
                                        :uri="germplasm.accession"
                                ></opensilex-LabelUriView>
                            </template>
                        </opensilex-Card>
                        <opensilex-Card label="GermplasmDetails.additionalInfo" icon="ik#ik-clipboard"
                                        v-if="addInfo.length != 0">
                            <template v-slot:body>
                                <b-table
                                        ref="tableAtt"
                                        striped
                                        hover
                                        small
                                        responsive
                                        :fields="attributeFields"
                                        :items="addInfo"
                                >
                                    <template v-slot:head(attribute)="data">{{$t(data.label)}}</template>
                                    <template v-slot:head(value)="data">{{$t(data.label)}}</template>
                                </b-table>
                            </template>
                        </opensilex-Card>
                    </b-col>
                    <b-col>
                        <opensilex-Card label="GermplasmDetails.experiment" icon="ik#ik-clipboard">
                            <template v-slot:body>
                                <opensilex-TableAsyncView
                                        ref="tableRef"
                                        :searchMethod="loadExperiments"
                                        :fields="expFields"
                                        defaultSortBy="label"
                                >
                                    <template v-slot:cell(uri)="{data}">
                                        <opensilex-UriLink
                                                :uri="data.item.uri"
                                                :to="{path: '/experiment/details/'+ encodeURIComponent(data.item.uri)}"
                                        ></opensilex-UriLink>
                                    </template>
                                </opensilex-TableAsyncView>
                            </template>
                        </opensilex-Card>
                    </b-col>
                </b-row>

                <opensilex-AnnotationList
                        v-else-if="isAnnotationTab()"
                        ref="annotationList"
                        :target="uri"
                        :displayTargetColumn="false"
                        :enableActions="true"
                        :modificationCredentialId="credentials.CREDENTIAL_PROJECT_MODIFICATION_ID"
                        :deleteCredentialId="credentials.CREDENTIAL_PROJECT_DELETE_ID"
                        @onEdit="annotationModalForm.showEditForm($event)"
                ></opensilex-AnnotationList>

            </template>
        </opensilex-PageContent>
        <opensilex-ModalForm
                ref="germplasmForm"
                component="opensilex-GermplasmForm"
                editTitle="udpate"
                icon="ik#ik-user"
                modalSize="lg"
                @onUpdate="update"
        ></opensilex-ModalForm>
    </div>
</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {
        GermplasmGetSingleDTO,
        GermplasmCreationDTO,
        GermplasmService,
        ExperimentGetListDTO,
    } from "opensilex-core/index";
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
    import AnnotationModalForm from "../annotations/form/AnnotationModalForm.vue";
    import AnnotationList from "../annotations/list/AnnotationList.vue";

    @Component
    export default class GermplasmDetails extends Vue {
        $opensilex: any;
        $route: any;
        $store: any;
        $router: any;
        $t: any;
        $i18n: any;
        service: GermplasmService;

        uri: string = null;
        addInfo = [];

        @Ref("modalRef") readonly modalRef!: any;

        @Ref("annotationModalForm") readonly annotationModalForm!: AnnotationModalForm;
        @Ref("annotationList") readonly annotationList!: AnnotationList;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        isDetailsTab() {
            return this.$route.path.startsWith("/germplasm/details/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/germplasm/annotations/");
        }

        germplasm: GermplasmGetSingleDTO = {
            uri: null,
            name: null,
            type: null,
            typeLabel: null,
            species: null,
            speciesLabel: null,
            variety: null,
            varietyLabel: null,
            accession: null,
            accessionLabel: null,
            institute: null,
            code: null,
            productionYear: null,
            comment: null,
            attributes: null,
        };

        created() {
            this.service = this.$opensilex.getService("opensilex.GermplasmService");
            this.uri = decodeURIComponent(this.$route.params.uri);
            this.loadGermplasm(this.uri);
        }

        loadGermplasm(uri: string) {
            this.service
                .getGermplasm(uri)
                .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
                    this.germplasm = http.response.result;
                    this.loadExperiments;
                    this.getAddInfo();
                })
                .catch(this.$opensilex.errorHandler);
        }

        expFields = [
            {
                key: "uri",
                label: "component.experiment.uri",
                sortable: true,
            },
            {
                key: "label",
                label: "component.experiment.label",
                sortable: true,
            },
        ];

        experiments = [];

        @Ref("table") readonly table!: any;

        loadExperiments(options) {
            return this.service.getGermplasmExperiments(
                this.uri,
                options.orderBy,
                options.currentPage,
                options.pageSize
            );
        }

        attributeFields = [
            {
                key: "attribute",
                label: "GermplasmDetails.attribute",
            },
            {
                key: "value",
                label: "GermplasmDetails.value",
            },
        ];

        @Ref("tableAtt") readonly tableAtt!: any;

        getAddInfo() {
            this.addInfo = []
            for (const property in this.germplasm.attributes) {
                let tableData = {
                    attribute: property,
                    value: this.germplasm.attributes[property],
                };
                this.addInfo.push(tableData);
            }
        }

        @Ref("germplasmForm") readonly germplasmForm!: any;

        updateGermplasm() {
            this.germplasmForm.getFormRef().getAttributes(this.germplasm);
            this.germplasmForm.showEditForm(this.germplasm);
        }

        update() {
            this.getAddInfo();
            this.$emit("onUpdate");
        }


        deleteGermplasm() {
            this.service
                .deleteGermplasm(this.germplasm.uri)
                .then(() => {
                    let message =
                        this.$i18n.t("GermplasmView.title") +
                        " " +
                        this.germplasm.uri +
                        " " +
                        this.$i18n.t("component.common.success.delete-success-message");
                    this.$opensilex.showSuccessToast(message);
                    this.$router.go(-1)
                })
                .catch(this.$opensilex.errorHandler);
        }

        updateAnnotations() {
            this.$nextTick(() => {
                this.annotationList.refresh();
            });
        }

    }
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
    GermplasmDetails:
        title: Germplasm
        description: Detailed Information
        info: Germplasm Information
        experiment: Associated experiments
        document: Associated documents
        uri: URI
        name: Name
        rdfType: Type
        species: Species
        variety: Variety
        accession: Accession
        institute: Institute
        year: Year
        comment: Comment
        backToList: Go back to Germplasm list
        code: Code
        synonyms: Synonyms
        additionalInfo: Additional information
        attribute: Attribute
        value: Value

fr:
    GermplasmDetails:
        title: Ressource génétique
        description: Information détaillées
        info: Informations générales
        experiment: Expérimentations associées
        document: Documents associées
        uri: URI
        name: Nom
        rdfType: Type
        species: Espèce
        variety: Variété
        accession: Accession
        institute: Institut
        year: Année
        comment: Commentaire
        backToList: Retourner à la liste des germplasm
        code: Code
        synonyms: Synonymes
        additionalInfo: Informations supplémentaires
        attribute: Attribut
        value: Valeur
</i18n>