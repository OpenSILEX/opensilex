<template>
    <div v-if="selected && selected.uri">
        <b-card>
            <template v-slot:header v-if="globalView">
                <h3>{{ $t("component.common.informations") }}:</h3>
                <div class="card-header-right">
                    <b-button-group>
                        <opensilex-FavoriteButton
                                :uri="selected.uri"
                        ></opensilex-FavoriteButton>
                        <opensilex-EditButton
                                v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_MODIFICATION_ID
                )
              "
                                @click="soForm.editScientificObject(selected.uri)"
                                label="ExperimentScientificObjects.edit-scientific-object"
                                :small="true"
                        ></opensilex-EditButton>
                        <opensilex-DeleteButton
                                v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_SCIENTIFIC_OBJECT_DELETE_ID
                )
              "
                                label="ExperimentScientificObjects.delete-scientific-object"
                                @click="deleteScientificObject(selected.uri)"
                                :small="true"
                        ></opensilex-DeleteButton>
                    </b-button-group>
                    <opensilex-ScientificObjectForm
                            ref="soForm"
                            @onUpdate="$emit('onUpdate', selected.uri)"
                            @onCreate="$emit('onCreate', selected.uri)"
                    ></opensilex-ScientificObjectForm>
                </div>
            </template>

            <!-- URI -->
            <opensilex-UriView
                    v-if="withBasicProperties"
                    :uri="selected.uri"
            ></opensilex-UriView>
            <!-- Name -->
            <opensilex-StringView
                    v-if="withBasicProperties"
                    :value="selected.name"
                    label="component.common.name"
            ></opensilex-StringView>
            <!-- Type -->
            <opensilex-TypeView
                    v-if="withBasicProperties"
                    :type="selected.rdf_type"
                    :typeLabel="selected.rdf_type_name"
            ></opensilex-TypeView>

            <!--Last Position-->
            <opensilex-StringView v-if="withBasicProperties && selected.location.endDate && loadFacility"
                                  label="Event.lastPosition">
                <!-- Position detail -->
                <span>{{ new Date(selected.location.endDate).toLocaleString() }}</span>
                <ul>
                    <li v-if="selected.location.to">{{ customFacilityText(selected.location) }}</li>
                    <li v-if="selected.location.x || selected.location.y || selected.location.z">
                        {{ customCoordinatesText(selected.location) }}
                    </li>
                    <li v-if="selected.location.text">{{ selected.location.text }}</li>
                    <li v-if="selected.location.geojson">
                        <opensilex-GeometryCopy label="" :value="selected.location.geojson">
                        </opensilex-GeometryCopy>
                    </li>
                </ul>
            </opensilex-StringView>
            <!-- Warning if the endDate is equal to 1970 == default date for facility geometry from migration-->
            <b-alert
                    v-if="selected.location.endDate === DEFAULT_DATE"
                    variant="warning"
                    show
            >
                {{ $t("component.facility.warning.facility-default-date") }}
            </b-alert>

            <!-- Relations -->
            <opensilex-OntologyObjectProperties
                    :selected="selected"
                    :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
                    :relations="relations"
                    :ignoredProperties="['vocabulary:isHosted']"
                    :additionalFieldProps="{ experiment }"
            ></opensilex-OntologyObjectProperties>

            <!-- Metadata -->
            <opensilex-MetadataView
                    v-if="selected.publisher && selected.publisher.uri"
                    :publisher="selected.publisher"
                    :publicationDate="selected.publication_date"
                    :lastUpdatedDate="selected.last_updated_date"
            ></opensilex-MetadataView>
        </b-card>

        <b-card v-for="(value, index) in objectByContext" :key="index">
            <template v-slot:header>
                <h3>
                    {{ $t("component.experiment.view.title") }}:
                    <opensilex-UriLink
                            :allowCopy="false"
                            :to="{
              path:
                '/experiment/details/' + encodeURIComponent(value.experiment),
            }"
                            :value="value.experiment_name"
                    ></opensilex-UriLink>
                </h3>
            </template>
            <!-- Name -->
            <opensilex-StringView
                    :value="value.name"
                    label="component.common.name"
            ></opensilex-StringView>
            <!-- Type -->
            <opensilex-TypeView
                    v-if="selected.rdf_type !== value.rdf_type"
                    :type="value.rdf_type"
                    :typeLabel="value.rdf_type_name"
            ></opensilex-TypeView>

            <!-- Relations -->
            <opensilex-OntologyObjectProperties
                    :selected="selected"
                    :parentType="oeso.SCIENTIFIC_OBJECT_TYPE_URI"
                    :relations="value.relations"
                    :ignoredProperties="['vocabulary:isHosted']"
                    :additionalFieldProps="{ experiment: value.experiment }"
            ></opensilex-OntologyObjectProperties>

            <!--Last XP Position -->
            <opensilex-StringView v-if="value.location && value.location.endDate && loadFacility"
                                  label="Event.lastPosition">
                <!-- Position detail &ndash-->
                <span>{{ new Date(value.location.endDate).toLocaleString() }}</span>
                <ul>
                    <li v-if="value.location.to">{{ customFacilityText(value.location) }}</li>
                    <li v-if="value.location.x || value.location.y || value.location.z">
                        {{ customCoordinatesText(value.location) }}
                    </li>
                    <li v-if="value.location.text">{{ value.location.text }}</li>
                    <li v-if="value.location.geojson">
                        <opensilex-GeometryCopy label="" :value="value.location.geojson">
                        </opensilex-GeometryCopy>
                    </li>
                </ul>
            </opensilex-StringView>
            <!-- Warning if the endDate is equal to 1970 == default date for facility geometry from migration-->
            <b-alert
                    v-if="selected.location.endDate === DEFAULT_DATE"
                    variant="warning"
                    show
            >
                {{ $t("component.facility.warning.facility-default-date") }}
            </b-alert>

            <!-- Metadata -->
            <opensilex-MetadataView
                    v-if="value.publisher && value.publisher.uri"
                    :publisher="value.publisher"
                    :publicationDate="value.publication_date"
                    :lastUpdatedDate="value.last_updated_date"
            ></opensilex-MetadataView>
        </b-card>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {ScientificObjectDetailByExperimentsDTO} from "../../../../../opensilex-core/front/src/lib";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";

@Component
export default class ScientificObjectDetailProperties extends Vue {
    $opensilex: OpenSilexVuePlugin;

    @Ref("soForm") readonly soForm!: any;

    @Prop()
    selected: ScientificObjectDetailByExperimentsDTO;
    @Prop({default: () => [],})
    objectByContext: Array<ScientificObjectDetailByExperimentsDTO>;
    @Prop({default: false,})
    globalView;
    @Prop({default: true,})
    withBasicProperties;
    @Prop({default: null})
    experiment;

    orgaService: OrganizationsService;
    facilityLabels: Map<String, String> = new Map<String, String>();
    loadFacility: boolean = false;
    relations: Array<RDFObjectRelationDTO> = [];
    private readonly DEFAULT_DATE: string = "1970-01-01T00:00:00Z"

    mounted() {
        if (this.selected) {
            this.onSelectionChange();
        }
    }

    get oeso() {
        return this.$opensilex.Oeso;
    }

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    @Watch("selected")
    onSelectionChange() {
        this.$opensilex.disableLoader();
        if (this.globalView) {
            this.relations = this.selected.relations;
        } else {
            this.relations = this.selected.relations;
        }
    }

    created() {
        this.orgaService = this.$opensilex.getService("opensilex.OrganizationsService");
        this.getFacilityLabels();
    }

    getFacilityLabels() {
        let facilitiesUris = [];
        //For global
        if (this.selected.location.to && this.facilityLabels.get(this.selected.location.to) == null) {
            facilitiesUris.push(this.selected.location.to)
            if (this.selected.location.from && this.facilityLabels.get(this.selected.location.from) == null) {
                facilitiesUris.push(this.selected.location.from)
            }
        }
        //For XP
        this.objectByContext.forEach(context => {
            if (context.location.to && this.facilityLabels.get(context.location.to) == null) {
                facilitiesUris.push(context.location.to)
                if (context.location.from && this.facilityLabels.get(context.location.from) == null) {
                    facilitiesUris.push(context.location.from)
                }
            }
        })

        if (facilitiesUris.length > 0) {
            this.orgaService.getFacilitiesByURI(facilitiesUris)
                    .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                        http.response.result.forEach(facility => {
                            this.facilityLabels.set(facility.uri, facility.name);
                        })
                    }).finally(() => {
                this.loadFacility = true
            })
        }
    }

    deleteScientificObject(uri) {
        let scientificObjectsService = this.$opensilex.getService<ScientificObjectsService>(
                "opensilex.ScientificObjectsService"
        );
        scientificObjectsService
                .deleteScientificObject(uri)
                .then(() => {
                    this.$router.push({
                        path: "/scientific-objects",
                    });
                })
                .catch(this.$opensilex.errorHandler);
    }

    customCoordinatesText(position: any): string {
        if (!position) {
            return undefined;
        }

        let customCoordinates = "";

        if (position.x) {
            customCoordinates += "X:" + position.x;
        }
        if (position.y) {
            if (customCoordinates.length > 0) {
                customCoordinates += ", ";
            }
            customCoordinates += "Y:" + position.y;
        }
        if (position.z) {
            if (customCoordinates.length > 0) {
                customCoordinates += ", ";
            }
            customCoordinates += "Z:" + position.z;
        }

        if (customCoordinates.length == 0) {
            return undefined;
        }
        return customCoordinates;
    }

    customFacilityText(item) {
        let customCoordinates = "";

        if (item.to) {
            customCoordinates += this.facilityLabels.get(item.to);
        }
        if (item.from) {
            customCoordinates += " (" + this.$i18n.t("Position.from") + " : " + this.facilityLabels.get(item.from) + ")";
        }
        if (customCoordinates.length == 0) {
            return undefined;
        }

        return customCoordinates;
    }
}
</script>

<i18n>
en:
    Event:
        lastPosition: Last position
        from: from
fr:
    Event:
        lastPosition: Dernière position
        from: à partir de
</i18n>