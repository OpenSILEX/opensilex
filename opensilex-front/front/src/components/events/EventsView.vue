<template>
    <opensilex-EventList
            ref="eventList"
            :enableActions="true"
            :columnsToDisplay="new Set(['type','start','end','description', 'targets'])"
            :displayTitle="false"
            :displayFilters="true"
            :maxPageSize=10
    ></opensilex-EventList>
</template>

<script lang="ts">
    import { Component, Ref } from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
    import {
        FactorCreationDTO,
        FactorsService,
        FactorGetDTO,
        FactorDetailsGetDTO,
        FactorUpdateDTO,
    } from "opensilex-core/index";
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
</style>

