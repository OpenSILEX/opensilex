<template>
  <div class="container-fluid">
    <opensilex-PageContent
    class="eventsContent">
      <template v-slot>
        <opensilex-EventList
            ref="eventList"
            :enableActions="true"
            :columnsToDisplay="new Set(['type','start','end','description', 'targets'])"
            :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
            :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
            :displayTitle="false"
            :isExperimentalFeature="false"
        ></opensilex-EventList>
      </template>
    </opensilex-PageContent>
  </div>
</template>


<script lang="ts">
    import { Component, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import EventList from "./list/EventList.vue";
    import {EventsService} from "opensilex-core/api/events.service";

    @Component
    export default class EventsView extends Vue {
        $opensilex: any;
        $store: any;
        service: EventsService;
        $t: any;
        $i18n: any;
        $router: any;

        @Ref("eventList") readonly eventList!: EventList;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }


        created() {
            this.service = this.$opensilex.getService("opensilex.EventsService");
        }

    }
</script>


<style scoped lang="scss">
.eventsContent {
  margin-top: -25px;
}
</style>


<i18n>
en:
  EventsView:
    description: Manage and configure events
fr:
  EventsView:
    description: Gérer et configurer les événementS
</i18n>