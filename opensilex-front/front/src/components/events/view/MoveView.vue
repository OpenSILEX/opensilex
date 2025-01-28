<template>
    <div v-if="event" class="card-body">

        <br>
        <p class="h5">{{ $t("Move.location") }} </p>
        <hr/>

        <opensilex-StringView v-if="loadFacility" label="Position.from"
                              :value="event.location.from ? customFacilityText(event.location.from) : '' "></opensilex-StringView>
        <opensilex-StringView label="Position.to"
                              :value="event.location.to ? customFacilityText(event.location.to) : '' "></opensilex-StringView>

        <div v-if="hasPosition(event)">
            <br>
            <p class="h5"> {{ $t("Position.title") }}</p>
            <hr/>
            <opensilex-PositionsView
                    :positions="event"
                    :positionsUriLabels="positionsUriLabels"
                    :positionsUriPaths="positionsUriPaths"
            >
            </opensilex-PositionsView>
        </div>
    </div>

</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {MoveDetailsDTO} from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";

@Component
export default class MoveView extends Vue {
    $opensilex: OpenSilexVuePlugin;
    orgaService: OrganizationsService;

    @Prop({default: () => MoveView.getEmptyForm})
    event: MoveDetailsDTO;
    @Prop()
    positionsUriLabels;
    @Prop()
    positionsUriPaths;


    facilityLabels: Map<String, String> = new Map<String, String>();
    loadFacility: boolean = false;

    created() {
        this.orgaService = this.$opensilex.getService("opensilex.OrganizationsService");
        this.getFacilityLabels();
    }

    static getEmptyForm(): MoveDetailsDTO {
        return {
            uri: undefined,
            rdf_type: undefined,
            rdf_type_name: undefined,
            start: undefined,
            end: undefined,
            targets: [],
            description: undefined,
            publisher: undefined,
            is_instant: true,
            location: undefined,
        };
    }

    hasPosition(event): boolean {
        return event && event.location;
    }

    getFacilityLabels() {
        let facilitiesUris = [];
        if (this.event.location.to && this.facilityLabels.get(this.event.location.to) == null) {
            facilitiesUris.push(this.event.location.to)
            if (this.event.location.from && this.facilityLabels.get(this.event.location.from) == null) {
                facilitiesUris.push(this.event.location.from)
            }
        }

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

    customFacilityText(facility) {
        let customCoordinates = this.facilityLabels.get(facility);

        if (customCoordinates.length == 0) {
            return undefined;
        }

        return customCoordinates;
    }
}
</script>

