<!--
  - ******************************************************************************
  -                         DateTimeRangeForm.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2025.
  - Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
  -
  -
  - ******************************************************************************
  -
  -->

<template>
    <div>
        <div class="row">
            <div class="col" v-if="canBeInstant">
                <!--Is instant-->
                <opensilex-FormField
                    :required="true"
                    label="Event.is-instant"
                    helpMessage="Event.is-instant-help"
                >
                    <template v-slot:field="field">
                        <b-form-checkbox v-model="is_instant" switch @change="updateIsInstantFilter">
                        </b-form-checkbox>
                    </template>
                </opensilex-FormField>
            </div>
        </div>

        <div class="row">
            <div class="col" v-if="!is_instant || !canBeInstant">
                <opensilex-DateTimeForm
                    ref="startDateSelector"
                    :value.sync="start"
                    label="Event.start"
                    :maxDate="end"
                    :required="startRequired"
                    helpMessage="Event.start-help"
                ></opensilex-DateTimeForm>
            </div>

            <div class="col">
                <opensilex-DateTimeForm
                    ref="endDateSelector"
                    :value.sync="end"
                    label="Event.end"
                    :minDate="start"
                    :required="endRequired"
                    helpMessage="Event.end-help"
                ></opensilex-DateTimeForm>
            </div>

        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {PropSync, Prop, Ref} from "vue-property-decorator";

@Component({})
export default class DateTimeRangeForm extends Vue {

    //#region Props
    @PropSync("startDate")
    private start: string;

    @PropSync("endDate")
    private end: string;

    @PropSync("isInstant")
    private is_instant: boolean;

    @PropSync("start_required")
    private startRequired: boolean;

    @PropSync("end_required")
    private endRequired: boolean;

    @Prop({default: true})
    private canBeInstant: boolean;
    //endregion

    //#region hooks
    private mounted(){
        if(!this.canBeInstant){
            this.startRequired = true;
        }
    }
    //#endregion

    //#region eventHandlers
    private updateIsInstantFilter(){
        this.$emit('change');
        this.updateRequiredProps()
    }

    private updateRequiredProps(){
        if (this.end === "") {
            this.end = undefined
        }
        if (this.start === ""){
            this.start = undefined
        }

        if (this.is_instant) {
            this.endRequired = true;
            //Set startDate to undefined so it can't be falsely taken into account
            this.start = undefined;
        } else {
            if(this.start == undefined && this.end == undefined) {
                this.startRequired = true;
                this.endRequired = true;
            } else {
                this.startRequired = !!this.start;
                this.endRequired = !!this.end;
            }
        }
    }
    //#endregion

}
</script>

<style scoped lang="scss">

</style>