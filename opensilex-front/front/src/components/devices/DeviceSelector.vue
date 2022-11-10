<template>
  <opensilex-SelectForm v-if="renderComponent"
    ref="deviceSelector"
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

import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import {DeviceGetDTO, DevicesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";


@Component
export default class DeviceSelector extends Vue {

  $opensilex: any;
  $service: DevicesService;
  $store: any;

  renderComponent = true;

  @PropSync("value")
  deviceURIs;

  @Prop()
  type: string;

  @Prop({default: false})
  multiple;

  @Prop({default: false})
  required;

  @Prop({default: "component.menu.devices"})
  label;

  @Watch("type")
  onTypeChange() {
    this.renderComponent = false;
    this.$nextTick(() => {
      // Add the component back in
      this.renderComponent = true;
    });
  }

  dtoByUriCache: Map<string,DeviceGetDTO>;

  @Ref("deviceSelector") readonly deviceSelector!: any;

  created() {
    this.$service = this.$opensilex.getService("opensilex.DevicesService");
    this.dtoByUriCache = new Map();
  }

  search(query, page, pageSize) {

    return this.$service.searchDevices(
        this.type, // rdf_type filter
        true, // include_subtypes boolean
        query, // name filter
        undefined, // variable filter
        undefined, // year filter
        undefined, // existence_date filter
        undefined, // facility filter
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
      return this.$service
        .getDeviceByUris(devices)
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
    placeholder: Rechercher et sélectionner un ou plusieurs dispositif
    no-results-text: Aucun dispositif trouvé

</i18n>