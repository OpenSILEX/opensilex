<template>
    <div v-if="lang === 'fr'">
        <GroupVariablesHelpFR></GroupVariablesHelpFR>
    </div>
    <div v-else>
        <GroupVariablesHelpEN></GroupVariablesHelpEN>
    </div>
</template>

<script lang="ts">
    import { Component } from "vue-property-decorator";
    import Vue from "vue";
    import GroupVariablesHelpFR from "./doc/GroupVariablesHelpFR.md";
    import GroupVariablesHelpEN from "./doc/GroupVariablesHelpEN.md";

    Vue.component("GroupVariablesHelpEN", GroupVariablesHelpEN);
    Vue.component("GroupVariablesHelpFR", GroupVariablesHelpFR);

    @Component
    export default class GroupVariablesHelp extends Vue {
        $opensilex: any;
        $store: any;
        $t: any;
        $i18n: any;
        $router: any;

        get lang() {
            console.debug("Active language : " + this.$store.getters.language);
            return this.$store.getters.language;
        }

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }
        source = new Date().toLocaleTimeString();
    }
</script>

<style scoped lang="scss">
</style>

