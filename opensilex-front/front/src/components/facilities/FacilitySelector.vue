<template>
    <div>
        <opensilex-FormSelector
                :label="label"
                :selected.sync="facilitiesURI"
                :multiple="multiple"
                :helpMessage="helpMessage"
                :placeholder="placeholder"
                :searchMethod="searchFacilities"
                :itemLoadingMethod="loadFacilities"
                :conversionMethod="facilityToSelectNode"
                noResultsText="FacilitySelector.no-result"
                @select="select"
                @deselect="deselect"
        ></opensilex-FormSelector>
    </div>
</template>


<script lang="ts">
    import { Component, Prop, PropSync } from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
    import {OrganizationsService} from "opensilex-core/api/organizations.service";
    import { NamedResourceDTO, FacilityGetDTO } from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

    @Component
    export default class FacilitySelector extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $service: OrganizationsService;

        @PropSync("facilities", {default: () => []})
        facilitiesURI;

        @Prop()
        label;

        @Prop()
        multiple;

        @Prop()
        helpMessage;

        @Prop({default: "FacilitySelector.placeholder"})
        placeholder;

        @Prop({ default: false })
          required: boolean;      

        created() {
            this.$service = this.$opensilex.getService("opensilex.OrganizationsService");
        }

        searchFacilities(searchQuery) {
            return this.$service.minimalSearchFacilities(
                searchQuery,
                undefined,
                undefined,
                undefined,
                0
            ).then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                return http;
            }).catch(this.$opensilex.errorHandler);
        }
        

        loadFacilities(facilitiesUris) {
            if (!facilitiesUris || facilitiesUris.length == 0) {
                return undefined;
            }
            return this.$service
                .getFacilitiesByURI(facilitiesUris)
                .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
                    (http && http.response) ? http.response.result : undefined
                );
        }


        facilityToSelectNode(dto: NamedResourceDTO) {
            if(! dto){
                return undefined;
            }
            return {
                label: dto.name,
                id: this.$opensilex.getShortUri(dto.uri) // shortUri is needed to avoid auto deselection problem on selectors with  both shorts and long URIs
            };
        }

        select(value) {
            this.$emit("select", value);
        }

        deselect(value) {
            this.$emit("deselect", value);
        }

        clear(){
            this.$emit("clear");
        }
    }
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    FacilitySelector:
        placeholder: Search and select a facility
        no-result: No facility found
fr:
    FacilitySelector:
        placeholder: "Rechercher et selectionner une installation"
        no-result: "Aucune installation trouvée"
</i18n>
