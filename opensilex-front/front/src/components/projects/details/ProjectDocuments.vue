<template>
  <div>
    <div class="row">
      <div class="col col-xl-12" style="min-width: 400px">
        <opensilex-Card label="Documents" icon="ik#ik-clipboard">
          <template v-slot:rightHeader>
            <!--    <opensilex-Button
              v-if="user.hasCredential(credentials.CREDENTIAL_PROJECT_MODIFICATION_ID)"
              @click="$emit('onEdit', project.uri)"
              label="component.project.update"
              :small="true"
              icon="fa#ellipsis-v"
            ></opensilex-Button>-->
            <opensilex-Icon icon="fa#ellipsis-v"></opensilex-Icon>
          </template>
          <template v-slot:body></template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import moment from "moment";
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
import {
  ProjectGetDetailDTO,
  ExperimentGetListDTO,
  ExperimentsService,
  ProjectsService
} from "opensilex-core/index";
import { SecurityService, UserGetDTO } from "opensilex-security/index";

@Component
export default class ProjectDocuments extends Vue {
  $opensilex: any;
  $route: any;
}
</script>

<style  lang="scss">
</style>

