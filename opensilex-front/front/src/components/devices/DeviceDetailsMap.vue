<template>
  <div class="container-fluid">
<!--Name and type -->
  <div v-if="showName">
    <opensilex-StringView
        label="component.common.name"
    ></opensilex-StringView>
    <opensilex-UriLink v-if="showName"
                       :to="{ path: '/device/details/' + encodeURIComponent(selected.details.uri)}"
                       :uri="selected.details.uri"
                       :value="selected.details.name + ' (' + selected.details.rdf_type_name.bold() + ')'"
                       target="_blank"
    ></opensilex-UriLink>
    <p>
      <a id="show" v-on:click="isViewAllInformation = !isViewAllInformation">{{
          $t(
              isViewAllInformation
                  ? "ScientificObjectDetailMap.seeMoreInformation"
                  : "ScientificObjectDetailMap.viewAllInformation"
          )
        }}</a>
    </p>
  </div>
<!-- start_up and removal-->
    <div v-if=" isViewAllInformation || !showName">
      <opensilex-StringView
          v-if="selected.details.start_up"
          :value="selected.details.start_up"
          label="device.start_up"
      ></opensilex-StringView>
      <opensilex-StringView
          v-if="selected.details.removal"
          :value="selected.details.removal"
          label="device.removal"
      ></opensilex-StringView>
    </div>
    <!--Last Position-->
    <opensilex-StringView  v-if="isViewAllInformation || !showName" label="Event.position">
      <!--Position detail-->
      <span v-if="position.move_time">{{new Date(position.move_time).toLocaleString()}}</span>
      <ul>
        <li v-if="position.to">{{position.to.name}}</li>
        <li v-if="position.position && position.position.x || position.position.y || position.position.z">{{customCoordinatesText(position.position)}}</li>
        <li v-if="position.position && position.position.text">{{position.position.text}}</li>
        <li v-if="position.position && position.position.point">
          <opensilex-GeometryCopy label="" :value="position.position.point">
          </opensilex-GeometryCopy>
        </li>
      </ul>
    </opensilex-StringView>
  </div>
</template>

<script lang="ts">

import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import {MoveDetailsDTO, PositionGetDTO } from 'opensilex-core/index';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import {EventsService} from "opensilex-core/api/events.service";

@Component
export default class DeviceDetailsMap extends Vue {
  @Prop()
  selected;
  @Prop()
  showName;
  isViewAllInformation: boolean = false;
  $opensilex: OpenSilexVuePlugin;
  moveService: EventsService;
  position: PositionGetDTO = {
    event: null,
    from: null,
    to: null,
    move_time: null,
    position: {
      point: null,
      text: null,
      x:null,
      y:null,
      z:null
    }
  };

  created() {
    this.moveService = this.$opensilex.getService("opensilex.EventsService");
    this.loadPosition();
  }

  loadPosition(){
    this.moveService.getMoveEvent(this.selected.event)
        .then((http: HttpResponse<OpenSilexResponse<MoveDetailsDTO>> ) => {
           this.position =  {
                event:  http.response.result.uri,
                from: http.response.result.from,
                to: http.response.result.to,
                move_time: http.response.result.end,
                position: {
                  point: http.response.result.targets_positions[0].position.point,
                  text: http.response.result.targets_positions[0].position.text,
                  x:http.response.result.targets_positions[0].position.x,
                  y:http.response.result.targets_positions[0].position.y,
                  z:http.response.result.targets_positions[0].position.z
                }
            };
      }).catch(this.$opensilex.errorHandler);
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
}
</script>

<style scoped>

#show {
  color: #007bff;
  cursor: pointer;
}

</style>
<i18n>
en:
  device:
    start_up: Start up
    removal: Removal
  Event:
    position: Geolocated position
fr:
  device:
    start_up: Date de mise en service
    removal: Date de mise hors service
  Event:
    position: Position géolocalisée
</i18n>