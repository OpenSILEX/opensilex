<template>
    <opensilex-FormField
            :required="required"
            :label="label"
            :helpMessage="helpMessage"
    >
        <template v-slot:field="field">
            <datetime
                    v-model="datetime"
                    :type="type"
                    :zone="zone"
                    :value-zone="valueZone"
                    :flow="flow"
                    :format="format"
                    @input="$emit('input', $event)"
                    @close="$emit('close', $event)"
            >
            </datetime>
        </template>
    </opensilex-FormField>
</template>

<script lang="ts">
    import {
        Component,
        Prop,
        PropSync
    } from "vue-property-decorator";
    import Vue from "vue";

    @Component
    export default class DateTimeForm extends Vue {

        $opensilex: any;

        @Prop()
        label: string;

        @Prop()
        helpMessage: string;

        @Prop()
        placeholder: string;

        @PropSync("required")
        isRequired: boolean;

        @Prop({default: false})
        disabled: boolean;

        @PropSync("value")
        datetime: string;

        @Prop({default: "datetime"})
        type: string;

        @Prop({default: "local"})
        zone: string;

        @Prop({default: "local"})
        valueZone: string;

        @Prop({default: "dd-MMM-yyyy HH:mm ZZ"})
        format: string;

        flow: Array<string> = ['date','time'];

    }
</script>

<style scoped lang="scss">
</style>

