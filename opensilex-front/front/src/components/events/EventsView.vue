<template>
  <div class="container-fluid">
    <PageContent
    class="eventsContent">
      <template v-slot>
        <EventList
            ref="eventList"
            :enableActions="true"
            :columnsToDisplay="new Set(['type','start','end','description', 'targets'])"
            :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
            :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID"
            :displayTitle="false"
            :isExperimentalFeature="false"
        ></EventList>
      </template>
    </PageContent>
  </div>
</template>


<script setup lang="ts">
    import { computed, inject, Ref, useTemplateRef } from "vue";
    import Vue from "vue";
    import EventList from "./list/EventList.vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import { EventsService } from "opensilex-core";
import { OpenSilexStore } from '../../models/Store';
    
    const store = useStore() as OpenSilexStore;
    const router = useRouter()
    const { t } = useI18n()
    const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>('$opensilex')
    const eventsService = opensilex.getService<EventsService>('opensilex.EventsService')

    const eventList = useTemplateRef<InstanceType<typeof EventList>>('eventList')    
    
    const user = computed(() => store.state.user)
    const credentials = computed(() => store.state.credentials)

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