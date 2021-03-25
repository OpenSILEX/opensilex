<template>
    <div>
        <opensilex-SelectForm
                :label="label"
                :selected.sync="facilitiesURI"
                :multiple="multiple"
                :helpMessage="helpMessage"
                :placeholder="placeholder"
                :searchMethod="searchFacilities"
                :itemLoadingMethod="loadFacilities"
                :conversionMethod="facilityToSelectNode"
                noResultsText="InfrastructureFacilitySelector.no-result"
                @select="select"
                @deselect="deselect"
        ></opensilex-SelectForm>
    </div>

</template>


<script lang="ts">
    import { Component, Prop, PropSync } from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
    import {OrganisationsService} from "opensilex-core/api/organisations.service";
    import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
    import {InfrastructureFacilityGetDTO} from "opensilex-core/model/infrastructureFacilityGetDTO";

    @Component
    export default class InfrastructureFacilitySelector extends Vue {

        $opensilex: any;
        $service: OrganisationsService;

        @PropSync("facilities", {default: () => []})
        facilitiesURI;

        @Prop()
        label;

        @Prop()
        multiple;

        @Prop()
        helpMessage;

        @Prop({default: "InfrastructureFacilitySelector.placeholder"})
        placeholder;

        facilitiesByUriCache: Map<string, NamedResourceDTO>;

        created() {
            this.$service = this.$opensilex.getService("opensilex.OrganisationsService");
            this.facilitiesByUriCache = new Map();
        }

        searchFacilities(searchQuery, page, pageSize) {

            return this.$service.searchInfrastructureFacilities(
                searchQuery, //name
                undefined,
                page,
                pageSize
            ).then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {

                if (http && http.response) {
                    this.facilitiesByUriCache.clear();
                    http.response.result.forEach(dto => {
                        this.facilitiesByUriCache.set(dto.uri, dto);
                    })
                }
                return http;
            }).catch(this.$opensilex.errorHandler);
        }

        loadFacilities(facilitiesUris) {

            if (!facilitiesUris || facilitiesUris.length == 0) {
                return undefined;
            }

            // if no facilities have been loaded, then call the GET{uris} service or just return facilities if they are DTOs

            if (this.facilitiesByUriCache.size == 0) {

                let facilitiesDTOs = [];
                facilitiesUris.forEach(facility => {

                    // if facility are object with an already filled name and uri, then no need to call service
                    if (facility.name && facility.name.length > 0 && facility.uri && facility.uri.length > 0) {
                        facilitiesDTOs.push(facility);
                    }
                })

                // if facilities are objects then just return them
                if (facilitiesDTOs.length > 0) {
                    return facilitiesDTOs;
                }

                return this.$service.getFacilitiesByURI(facilitiesUris)
                    .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) =>
                        (http && http.response) ? http.response.result : undefined
                    );
            }

            // if facilities have been loaded, then it's not needed to call the GET{uri} service just for retrieve the facility name
            // since the name is returned by the SEARCH facilities service and the result is cached into facilitiesByUriCache

            let facilitiesToReturn = [];
            facilitiesUris.forEach(facility => {
                let loadedFacility = this.facilitiesByUriCache.get(facility);
                facilitiesToReturn.push(loadedFacility);
            });
            return facilitiesToReturn;

        }

        facilityToSelectNode(dto: NamedResourceDTO) {
            if(! dto){
                return undefined;
            }
            return {
                label: dto.name,
                id: dto.uri
            };
        }

        select(value) {
            this.$emit("select", value);
        }

        deselect(value) {
            this.$emit("deselect", value);
        }
    }
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    InfrastructureFacilitySelector:
        placeholder: Search and select a facility
        no-result: No facility found
fr:
    InfrastructureFacilitySelector:
        placeholder: "Rechercher et selectionner une installation"
        no-result: "Aucune installation trouvée"
</i18n>
