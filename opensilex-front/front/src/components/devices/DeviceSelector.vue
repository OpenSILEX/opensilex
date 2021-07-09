<template>
  <opensilex-SelectForm
      id="deviceSelector"
      :label="label"
      placeholder="DeviceSelector.placeholder"
      noResultsText="DeviceSelector.no-results-text"
      :selected.sync="deviceURIs"
      :multiple="multiple"
      :required="required"
      :searchMethod="search"
      :itemLoadingMethod="load"
      :conversionMethod="dtoToSelectNode"
      :key="lang"
      @clear="$emit('clear')"
      @select="$emit('select')"
      @deselect="$emit('deselect')"
      :showCount="true"
  ></opensilex-SelectForm>

</template>

<script lang="ts">

import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import {DevicesService} from "opensilex-core/index";
import {DeviceGetDTO} from "opensilex-core/model/deviceGetDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {ScientificObjectNodeDTO} from "opensilex-core/model/scientificObjectNodeDTO";


@Component
export default class DeviceSelector extends Vue {

  $opensilex: any;
  $service: DevicesService;
  $store: any;

  @PropSync("value")
  deviceURIs;

  @Prop({default: false})
  multiple;

  @Prop({default: false})
  required;

  @Prop({default: "component.menu.devices"})
  label;

  dtoByUriCache: Map<string,DeviceGetDTO>;

  created() {
    this.$service = this.$opensilex.getService("opensilex.DevicesService");
    this.dtoByUriCache = new Map();
  }

  search(query, page, pageSize) {

    return this.$service.searchDevices(
        undefined, // rdf_type filter
        undefined, // include_subtypes boolean
        query, // name filter
        undefined, // year filter
        undefined, // existence_date filter
        undefined, // brand filter
        undefined, // model filter
        undefined, // serial_number filter
        undefined, //metadata filter
        ["name=asc"],
        page,
        pageSize,
    ).then((http) => {

        if (http && http.response) {
            this.dtoByUriCache.clear();
            http.response.result.forEach(dto => {
                this.dtoByUriCache.set(dto.uri,dto);
            })
        }
        return http;

    }).catch(this.$opensilex.errorHandler);
  }

    load(devices) {

        if (!devices){
            return undefined;
        }

        if(! Array.isArray(devices)){
            devices = [devices];
        }else if(devices.length == 0){
            return undefined;
        }

        let dtosToReturn = [];

        if (this.dtoByUriCache.size == 0) {
            devices.forEach(device => {

                // if the device is an object (and not an uri) with an already filled name and uri, then no need to call service
                if (device.name && device.name.length > 0 && device.uri && device.uri.length > 0) {
                    dtosToReturn.push(device);
                }
            })

            // if all element to load are objects then just return them
            if (dtosToReturn.length == devices.length) {
                return dtosToReturn;
            }
        }else {

            // if object have already been loaded, then it's not needed to call the GET{uri} service just for retrieve the object name
            // since the name is returned by the SEARCH service and the result is cached into dtoByUriCache

            devices.forEach(device => {
                let loadedDto = this.dtoByUriCache.get(device);
                dtosToReturn.push(loadedDto);
            });

            return dtosToReturn;
        }

        return this.$service
            .getDeviceByUris(undefined,devices)
            .then((http: HttpResponse<OpenSilexResponse<Array<DeviceGetDTO>>>) => {
                return (http && http.response) ? http.response.result : undefined
            }).catch(this.$opensilex.errorHandler);

    }

  dtoToSelectNode(dto: DeviceGetDTO) {
    if (!dto) {
      return undefined;
    }
    return {label: dto.name, id: dto.uri};
  }

  get lang() {
    return this.$store.getters.language;
  }

}
</script>

<i18n>

en:
  DeviceSelector:
    placeholder: Search and select devices
    no-results-text: No device found
fr:
  DeviceSelector:
    placeholder: Rechercher et sélectionner un ou plusieurs équipements
    no-results-text: Aucun équipement trouvé

</i18n>