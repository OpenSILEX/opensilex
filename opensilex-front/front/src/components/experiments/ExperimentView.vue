<template>
    <div v-if="uri" class="container-fluid">
        <opensilex-PageHeader
                icon="ik#ik-layers"
                :title="name"
                description="component.experiment.view.title"
                class= "detail-element-header"
        ></opensilex-PageHeader>

        <opensilex-PageActions :tabs="true" :returnButton="true">
            <template v-slot>
                <b-nav-item
                        :active="isDetailsTab()"
                        :to="{ path: '/experiment/details/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.description") }}
                </b-nav-item>
                <b-nav-item
                        :active="isFactorsTab()"
                        :to="{ path: '/experiment/factors/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.factors") }}
                    <span
                        v-if="!factorsCountIsLoading && factors > 0"
                        class ="tabWithElements"
                    >
                        {{$opensilex.$numberFormatter.formateResponse(factors)}}
                    </span>
                </b-nav-item>
                <b-nav-item
                        :active="isScientificObjectsTab()"
                        :to="{path: '/experiment/scientific-objects/' + encodeURIComponent(uri),}"
                >{{ $t("ExperimentView.scientific-objects") }}
                    <span
                        v-if="!scientificObjectsCountIsLoading && scientificObjects > 0"
                        class="tabWithElements"
                    >
                        {{$opensilex.$numberFormatter.formateResponse(scientificObjects)}}
                    </span>
                </b-nav-item
                >
                <b-nav-item
                        :active="isDataTab()"
                        :to="{ path: '/experiment/data/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.data") }}
                    <span
                        v-if="!dataCountIsLoading && dataCount > 0"
                        class ="tabWithElements"
                    >
                        {{$opensilex.$numberFormatter.formateResponse(dataCount)}}
                    </span>
                </b-nav-item
                >
                <b-nav-item
                        :active="isDataVisualisation()"
                        :to="{ path: '/experiment/data-visualisation/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.data-visualisation") }}
                </b-nav-item
                >
                <b-nav-item
                        :active="isMap()"
                        :to="{ path: '/experiment/map/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.map") }}
                </b-nav-item>

                <b-nav-item
                        :active="isAnnotationTab()"
                        :to="{ path: '/experiment/annotations/' + encodeURIComponent(uri) }"
                >{{ $t("Annotation.list-title") }}
                    <span
                        v-if="!annotationsCountIsLoading && annotations > 0"
                        class ="tabWithElements"
                    >
                        {{$opensilex.$numberFormatter.formateResponse(annotations)}}
                    </span>
                </b-nav-item>
                
                <b-nav-item
                  :active="isDocumentTab()"
                  :to="{path: '/experiment/document/' + encodeURIComponent(uri)}"
                >{{ $t('ExperimentView.document') }}
                    <span
                        v-if="!documentsCountIsLoading && documents > 0"
                        class ="tabWithElements"
                    >
                        {{$opensilex.$numberFormatter.formateResponse(documents)}}
                    </span>
                </b-nav-item>



            </template>
        </opensilex-PageActions>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-ExperimentDetail
                        v-if="isDetailsTab()"
                        :uri="uri"
                ></opensilex-ExperimentDetail>
                <opensilex-ExperimentFactors
                        v-else-if="isFactorsTab()"
                        :uri="uri"
                ></opensilex-ExperimentFactors>
                <opensilex-ExperimentScientificObjects
                        v-else-if="isScientificObjectsTab()"
                        :uri="uri"
                ></opensilex-ExperimentScientificObjects>
                <opensilex-ExperimentData
                        v-else-if="isDataTab()"
                        :uri="uri"
                ></opensilex-ExperimentData>
                <opensilex-ExperimentDataVisualisation
                        v-else-if="isDataVisualisation()"
                        :uri="uri"
                        :elementName="name"
                ></opensilex-ExperimentDataVisualisation>

                <opensilex-MapView
                        v-else-if="isMap()"
                        :uri="uri"
                ></opensilex-MapView>

                <opensilex-DocumentTabList
                        v-else-if="isDocumentTab()"
                        :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
                        :uri="uri"
                ></opensilex-DocumentTabList>
                
                <opensilex-AnnotationList
                        v-else-if="isAnnotationTab()"
                        ref="annotationList"
                        :target="uri"
                        :displayTargetColumn="false"
                        :enableActions="true"
                        :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
                        :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
                ></opensilex-AnnotationList>

            </template>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
    // @ts-ignore
    import {ExperimentsService, ExperimentGetDTO} from "opensilex-core/index";
    // @ts-ignore
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
    import AnnotationList from "../annotations/list/AnnotationList.vue";

    import {AnnotationsService} from "opensilex-core/api/annotations.service";
    import {DocumentsService} from "opensilex-core/api/documents.service";
    import {FactorsService} from "opensilex-core/api/factors.service";
    import {DataService} from "opensilex-core/api/data.service";
    import {ScientificObjectsService} from "opensilex-core/index";

    @Component
    export default class ExperimentView extends Vue {
        $route: any;

        $opensilex: OpenSilexVuePlugin;
        service: ExperimentsService;
        uri = null;
        name: string = "";

        $AnnotationsService: AnnotationsService
        $DocumentsService: DocumentsService
        $FactorsService: FactorsService
        $DataService: DataService
        $ScientificObjectsService: ScientificObjectsService

        annotations: number;
        documents: number;
        factors: number;
        dataCount: number;
        scientificObjects: number;

        annotationsCountIsLoading: boolean = true;
        documentsCountIsLoading: boolean = true;
        factorsCountIsLoading: boolean = true;
        dataCountIsLoading: boolean = true;
        scientificObjectsCountIsLoading: boolean = true;

        @Ref("annotationList") readonly annotationList!: AnnotationList;

        created() {
            this.service = this.$opensilex.getService("opensilex.ExperimentsService");

            this.uri = decodeURIComponent(this.$route.params.uri);
            if (this.uri) {
                this.service
                    .getExperiment(this.uri)
                    .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
                        this.name = http.response.result.name;
                    })
                    .catch((error) => {
                        this.$opensilex.errorHandler(error);
                    });
            }
            this.$AnnotationsService = this.$opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
            this.$DocumentsService = this.$opensilex.getService<DocumentsService>("opensilex.DocumentsService");
            this.$DataService = this.$opensilex.getService<DataService>("opensilex.DataService");
            this.$FactorsService = this.$opensilex.getService<FactorsService>("opensilex.FactorsService");
            this.$ScientificObjectsService = this.$opensilex.getService<ScientificObjectsService>("opensilex.ScientificObjectsService");
            this.searchAnnotations();
            this.searchDocuments();
            this.searchData();
            this.searchFactors();
            this.searchScientificObjects();
        }

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        isDetailsTab() {
            return this.$route.path.startsWith("/experiment/details/");
        }

        isMap() {
            return this.$route.path.startsWith("/experiment/map/");
        }

        isFactorsTab() {
            return this.$route.path.startsWith("/experiment/factors/");
        }

        isScientificObjectsTab() {
            return this.$route.path.startsWith("/experiment/scientific-objects/");
        }

        isDataTab() {
            return this.$route.path.startsWith("/experiment/data/");
        }

        isDataVisualisation() {
            return this.$route.path.startsWith("/experiment/data-visualisation/");
        }

        isDocumentTab() {
          return this.$route.path.startsWith("/experiment/document/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/experiment/annotations/");
        }


        searchAnnotations() {
            return this.$AnnotationsService
            .countAnnotations(
            this.uri,
            undefined,
            undefined
            ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
                if (http && http.response){
                    this.annotations = http.response.result as number;
                    this.annotationsCountIsLoading = false;
                    return this.annotations
                }
            }).catch(this.$opensilex.errorHandler);
        }

        searchDocuments(){
            return this.$DocumentsService
            .countDocuments(
                this.uri,
                undefined,
                undefined
            ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
                if (http && http.response) {
                    this.documents = http.response.result as number;
                    this.documentsCountIsLoading = false;
                    return this.documents
                }
            }).catch(this.$opensilex.errorHandler);
        }

      searchData() {
        // Limit count of data for performance reasons        -->

        return this.$DataService
            .countData(
                undefined,
                undefined,
                undefined,
                [this.uri],
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                1000,
                undefined
            ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
              if (http && http.response) {
                this.dataCount = http.response.result as number;
                this.dataCountIsLoading = false;
                return this.dataCount
              }
            }).catch(this.$opensilex.errorHandler);
      }
        
        searchFactors(){
            return this.$FactorsService
            .countFactors(
                this.uri,
                undefined,
                undefined
            ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
                if (http && http.response){
                    this.factors = http.response.result as number;
                    this.factorsCountIsLoading = false;
                    return this.factors
                }
            }).catch(this.$opensilex.errorHandler);
        }

        searchScientificObjects(){
            return this.$ScientificObjectsService
            .countScientificObjects(
                this.uri
            ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
                if (http && http.response) {
                    this.scientificObjects = http.response.result as number;
                    this.scientificObjectsCountIsLoading = false;
                    return this.scientificObjects
                }
            }).catch(this.$opensilex.errorHandler);
        }
    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ExperimentView:
        description: Description
        scientific-objects: Scientific objects
        data: Data
        document: Documents
        factors: Factors
        map: Map
        data-visualisation: Visualization
fr:
    ExperimentView:
        description: Description
        scientific-objects: Objets scientifiques
        data: Données
        document: Documents
        factors: Facteurs
        map: Carte
        data-visualisation: Visualisation
</i18n>
