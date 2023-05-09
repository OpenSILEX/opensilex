<template>
    <div v-if="uri" class="container-fluid">
        <opensilex-PageHeader
                icon="ik#ik-globe"
                :title="name"
                description="component.facility.view.title"
                class= "detail-element-header"
        ></opensilex-PageHeader>

        <opensilex-PageActions :tabs="true" :returnButton="true">
            <template v-slot>
                <b-nav-item
                        :active="isDetailsTab()"
                        :to="{ path: '/facility/details/' + encodeURIComponent(uri) }"
                >{{ $t("FacilityView.details") }}
                </b-nav-item>

                <b-nav-item
                    :active="isDeviceTab()"
                    :to="{ path: '/facility/devices/' + encodeURIComponent(uri) }"
                >{{ $t("FacilityView.devices") }}
                </b-nav-item>

                <b-nav-item
                        :active="isAnnotationTab()"
                        :to="{ path: '/facility/annotations/' + encodeURIComponent(uri) }"
                >{{ $t("Annotation.list-title") }}
                </b-nav-item>
                
                <b-nav-item
                  :active="isDocumentTab()"
                  :to="{path: '/facility/document/' + encodeURIComponent(uri)}"
                >{{ $t('FacilityView.document') }}
                </b-nav-item>
            </template>
        </opensilex-PageActions>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-FacilityDetails
                        v-if="isDetailsTab()"
                        :uri="uri"
                ></opensilex-FacilityDetails>

                <opensilex-FacilityAssociatedDevices
                    v-if="isDeviceTab()"
                    :uri="uri"
                ></opensilex-FacilityAssociatedDevices>

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
    import {OrganizationsService, FacilityGetDTO} from "opensilex-core/index";
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
    import AnnotationList from "../annotations/list/AnnotationList.vue";
    import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
    import {Route} from "vue-router";

    @Component
    export default class FacilityView extends Vue {
        $route: Route;
        $opensilex: OpenSilexVuePlugin;

        service: OrganizationsService;

        uri = null;
        name: string = "";

        @Ref("annotationList") readonly annotationList!: AnnotationList;

        created() {
            this.service = this.$opensilex.getService("opensilex.OrganizationsService");

            this.uri = decodeURIComponent(this.$route.params.uri);
            if (this.uri) {
                this.service
                    .getFacility(this.uri)
                    .then((http: HttpResponse<OpenSilexResponse<FacilityGetDTO>>) => {
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
            return this.$route.path.startsWith("/facility/details/");
        }

        isDeviceTab() {
          return this.$route.path.startsWith("/facility/devices/");
        }

        isDocumentTab() {
          return this.$route.path.startsWith("/facility/document/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/facility/annotations/");
        }

    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    FacilityView:
        details: Description
        devices: Associated devices
        document: Documents
fr:
    FacilityView:
        details: Description
        devices: Appareils associés
        document: Documents
</i18n>
