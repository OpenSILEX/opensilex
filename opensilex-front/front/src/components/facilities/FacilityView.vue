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
                  :active="isOverviewTab()"
                  :to="{ path: '/facility/overview/' + encodeURIComponent(uri) }"
              > {{ $t("FacilityView.overview") }}
                <img
                    v-bind:src="$opensilex.getResourceURI('images/construction.png')"
                    class="wip-icon"
                    alt="work in progress"
                >
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

              <b-nav-item
                  :active="isPositionsTab()"
                  :to="{path: '/facility/positions/' + encodeURIComponent(uri)}"
              >{{ $t('Position.list-title') }}
                <span
                    v-if="!positionsCountIsLoading && positions > 0"
                    class="tabWithElements"
                >
                  {{ $opensilex.$numberFormatter.formateResponse(positions) }}
                </span>
              </b-nav-item>
            </template>
        </opensilex-PageActions>

        <opensilex-PageContent>
            <template v-slot>

                <opensilex-FacilityDetails
                    v-if="isDetailsTab()"
                    :uri="uri"
                ></opensilex-FacilityDetails>

              <opensilex-FacilityMonitoringView
                  v-if="isOverviewTab()"
                  :uri="uri"
              ></opensilex-FacilityMonitoringView>

              <opensilex-LocationList
                  v-if="isPositionsTab()"
                  :target="uri"
              ></opensilex-LocationList>

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
    import {LocationsService} from "opensilex-core/api/locations.service";

    @Component
    export default class FacilityView extends Vue {
        $route: Route;
        $opensilex: OpenSilexVuePlugin;
      service: OrganizationsService;
      locationsService: LocationsService;

        uri = null;
        name: string = "";
        positionsCountIsLoading: boolean = true;
      positions: number;

        @Ref("annotationList") readonly annotationList!: AnnotationList;

        created() {
          this.service = this.$opensilex.getService("opensilex.OrganizationsService");
          this.locationsService = this.$opensilex.getService<LocationsService>("opensilex.LocationsService");

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

          this.searchCountPositions();
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

        isOverviewTab() {
          return this.$route.path.startsWith("/facility/overview/");
        }

        isDocumentTab() {
          return this.$route.path.startsWith("/facility/document/");
        }

        isAnnotationTab() {
            return this.$route.path.startsWith("/facility/annotations/");
        }

        isPositionsTab() {
            return this.$route.path.startsWith("/facility/positions/");
        }

      searchCountPositions(){
        return this.locationsService
            .countLocations(this.uri)
            .then((http: HttpResponse<OpenSilexResponse<number>>) => {
              if (http && http.response){
                this.positions = http.response.result as number;
                this.positionsCountIsLoading = false;
                return this.positions
              }
            }).catch(this.$opensilex.errorHandler);
      }
    }
</script>

<style scoped lang="scss">

.wip-icon {
  float: right;
  padding-left: 5px;
  width: 28px;
}

</style>

<i18n>
en:
    FacilityView:
        details: Details
        overview: Monitoring
        document: Documents
fr:
    FacilityView:
        details: Détail
        overview: Supervision
        document: Documents
</i18n>
