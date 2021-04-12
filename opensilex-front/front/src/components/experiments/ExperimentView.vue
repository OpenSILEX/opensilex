<template>
    <div v-if="uri" class="container-fluid">
        <opensilex-PageHeader
                icon="ik#ik-layers"
                :title="name"
                description="component.experiment.view.title"
        ></opensilex-PageHeader>

        <opensilex-PageActions :tabs="true" :returnButton="true">
            <template v-slot>
                <b-nav-item
                        :active="isDetailsTab()"
                        :to="{ path: '/experiment/details/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.details") }}
                </b-nav-item>
                <b-nav-item
                        :active="isFactorsTab()"
                        :to="{ path: '/experiment/factors/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.factors") }}
                </b-nav-item>
                <b-nav-item
                        :active="isScientificObjectsTab()"
                        :to="{path: '/experiment/scientific-objects/' + encodeURIComponent(uri),}"
                >{{ $t("ExperimentView.scientific-objects") }}
                </b-nav-item
                >
                <b-nav-item
                        :active="isDataTab()"
                        :to="{ path: '/experiment/data/' + encodeURIComponent(uri) }"
                >{{ $t("ExperimentView.data") }}
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
                </b-nav-item>
                
                <b-nav-item
                  :active="isDocumentTab()"
                  :to="{path: '/experiment/document/' + encodeURIComponent(uri)}"
                >{{ $t('ExperimentView.document') }}
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

                <opensilex-MapView
                        v-else-if="isMap()"
                        :uri="uri"
                ></opensilex-MapView>

                <opensilex-DocumentTabList
                        v-else-if="isDocumentTab()"
                        :modificationCredentialId="credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
                        :uri="uri"
                ></opensilex-DocumentTabList>
                
                <opensilex-AnnotationList
                        v-else-if="isAnnotationTab()"
                        ref="annotationList"
                        :target="uri"
                        :displayTargetColumn="false"
                        :enableActions="true"
                        :modificationCredentialId="credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
                        :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
                ></opensilex-AnnotationList>

            </template>
        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ExperimentsService, ExperimentGetDTO} from "opensilex-core/index";

    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
    import AnnotationList from "../annotations/list/AnnotationList.vue";
    import AnnotationModalForm from "../annotations/form/AnnotationModalForm.vue";

    @Component
    export default class ExperimentView extends Vue {
        $route: any;

        $opensilex: any;
        service: ExperimentsService;
        uri = null;
        name: string = "";

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

        isDocumentTab() {
          return this.$route.path.startsWith("/experiment/document/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/experiment/annotations/");
        }

    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ExperimentView:
        details: Details
        scientific-objects: Scientific objects
        data: Data
        document: Documents
        factors: Factors
        map: Map
fr:
    ExperimentView:
        details: Détail
        scientific-objects: Objets scientifiques
        data: Données
        document: Documents
        factors: Facteurs
        map: Carte
</i18n>
