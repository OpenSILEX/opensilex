<template>
  <div class="container-fluid">
    <PageContent class="eventsContent">
      <template v-slot>
        <EventList ref="eventList" :enableActions="true"
          :columnsToDisplay="new Set(['type', 'start', 'end', 'description', 'targets'])"
          :modificationCredentialId="credentials.CREDENTIAL_EVENT_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_EVENT_DELETE_ID" :displayTitle="false"
          :isExperimentalFeature="false"></EventList>
      </template>
    </PageContent>
  </div>
</template>


<script setup lang="ts">
import { computed } from "vue";
import EventList from "./list/EventList.vue";
import { useStore } from "vuex";
import { OpenSilexStore } from '../../models/Store';

const store = useStore() as OpenSilexStore;
const credentials = computed(() => store.state.credentials)

</script>


<style scoped lang="scss">
.eventsContent {
  margin-top: -25px;
}
</style>
