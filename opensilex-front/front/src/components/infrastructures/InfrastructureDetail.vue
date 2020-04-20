<template>
  <div>
    <div class="card">
      <div class="card-header">
        <h3>
          <i class="ik ik-clipboard"></i>
          {{$t("component.infrastructure.detail")}}
        </h3>
      </div>
      <div class="card-body">
        <div v-if="selected">
          <div class="static-field">
            <span class="static-field-key">{{$t("component.common.uri")}}:</span>
            <span class="static-field-line">{{selected.uri}}</span>
          </div>
          <div class="static-field">
            <span class="static-field-key">{{$t("component.common.name")}}:</span>
            <span class="static-field-line">{{selected.name}}</span>
          </div>
          <div class="static-field">
            <span class="static-field-key">{{$t("component.common.type")}}:</span>
            <span class="static-field-line">
              <font-awesome-icon :icon="$opensilex.getRDFIcon(selected.type)" size="sm" />&nbsp;
              <span class="capitalize-first-letter">{{selected.typeLabel}}</span>&nbsp;
              <small>({{selected.type}})</small>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO,
  InfrastructureDeviceGetDTO,
  InfrastructureTeamDTO
} from "opensilex-core/index";
import { GroupCreationDTO, GroupUpdateDTO } from "opensilex-security/index";

@Component
export default class InfrastructureDetail extends Vue {
  $opensilex: any;

  @Prop()
  selected;
}
</script>

<style scoped lang="scss">
</style>

