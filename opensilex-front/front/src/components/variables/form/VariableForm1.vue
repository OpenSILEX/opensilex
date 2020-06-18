<template>
    <ValidationObserver ref="validatorRef">
        <!-- description -->
        <opensilex-TextAreaForm
                :value.sync="form.description"
                label="component.variable.comment"
        ></opensilex-TextAreaForm>


        <!-- dimension -->

        <b-form-group>
            <opensilex-FormInputLabelHelper label=component.variable.unit-dimension
                                            helpMessage="component.variable.unit-dimension-help">
            </opensilex-FormInputLabelHelper>
            <ValidationProvider :name="$t('component.variable.unit-dimension')"
                                v-slot="{ errors }">
                <multiselect
                        :placeholder="$t('component.variable.unit-dimension-placeholder')"
                        :limit="1"
                        :closeOnSelect="true"
                        v-model="form.dimension"
                        :options="dimensionList"
                        selectLabel=""
                        selectedLabel="X"
                        deselectLabel="X"
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                />
                <div class="error-message alert alert-danger">{{ errors[0] }}</div>
            </ValidationProvider>
        </b-form-group>


        <div class="row">
            <div class="col-lg-3">

                <!-- lower bound -->
                <opensilex-InputForm
                        :value.sync="form.lowerBound"
                        label="component.variable.lower-bound"
                        type="number"
                        placeholder="component.variable.lower-bound-placeholder"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-3">
                <!-- upper bound -->
                <opensilex-InputForm
                        :value.sync="form.upperBound"
                        label="component.variable.upper-bound"
                        type="number"
                        placeholder="component.variable.lower-bound-placeholder"
                ></opensilex-InputForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-3">
                <!-- synonym -->
                <opensilex-InputForm
                        :value.sync="form.synonym"
                        label="component.variable.synonym"
                        type="text"
                ></opensilex-InputForm>
            </div>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";

    import {VariableCreationDTO} from "opensilex-core/index";

    @Component
    export default class VariableForm1 extends Vue {
        $opensilex: any;

        dimensionList: Array<String> = ["volume", "surface", "time", "distance"];

        @Prop()
        editMode;

        @Prop({default: true})
        uriGenerated;

        @Ref("validatorRef") readonly validatorRef!: any;

        get user() {
            return this.$store.state.user;
        }

        @PropSync("form")
        variable: VariableCreationDTO;

        reset() {
            this.uriGenerated = true;
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }
    }
</script>
<style scoped lang="scss">
</style>
