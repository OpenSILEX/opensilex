<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col-lg-6">
                <!-- dimension -->
                <opensilex-SelectForm
                        label="VariableForm.dimension"
                        :selected.sync="form.dimension"
                        :multiple="false"
                        :options="dimensionList"
                        :conversionMethod="objectToSelectNode"
                        placeholder="VariableForm.dimension-placeholder"
                        helpMessage="VariableForm.dimension-help"
                ></opensilex-SelectForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <!-- synonym -->
                <opensilex-InputForm
                        :value.sync="form.synonym"
                        label="VariableForm.synonym"
                        type="text"
                ></opensilex-InputForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">

                <!-- trait uri-->
                <opensilex-InputForm
                        :value.sync="form.traitUri"
                        label="VariableForm.trait-uri"
                        type="text"
                        helpMessage="VariableForm.trait-uri-help"
                        placeholder="VariableForm.trait-uri-placeholder"
                        @required="isTraitUriFilled"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-6">
                <!-- trait name-->
                <opensilex-InputForm
                        :value.sync="form.traitName"
                        label="VariableForm.trait-name"
                        type="text"
                        helpMessage="VariableForm.trait-name-help"
                        placeholder="VariableForm.trait-name-placeholder"
                        @required="isTraitNameFilled"
                ></opensilex-InputForm>
            </div>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {VariableCreationDTO} from "opensilex-core/model/variableCreationDTO";


    @Component
    export default class VariableForm2 extends Vue {

        dimensionList: Array<String> = [];
        $i18n: any;

        objectToSelectNode(dto) {
            return {
                id: this.$i18n.t("VariableForm.dimension-values."+dto.label),
                label: this.$i18n.t("VariableForm.dimension-values."+dto.label)
            };
        }

        objectListToSelect(list) {
            let itemList = [];
            if (list) {
                for (let i in list) {
                    let baseItem: any = this.objectToSelectNode(list[i]);
                    let children = this.objectListToSelect(list[i].children);
                    if (children.length > 0) {
                        baseItem.children = children;
                    }
                    itemList.push(baseItem);
                }
            }
            return itemList;
        }

        created() {
            let dimensions = [
                {label: "volume", children: [{label: "m3"}, {label: "liter"}]},
                {label: "surface", children: [{label: "hectare"}, {label: "m2"}]},
                {label: "time", children: [{label: "second"}, {label: "minute"}, {label: "hour"}, {label: "day"}]},
                {label: "length", children: [{label: "cm"}, {label: "m"},  {label: "km"}]},
            ];

            this.dimensionList = this.objectListToSelect(dimensions);
        }

        @Prop()
        editMode;

        @Prop({default: true})
        useTraitUri;

        @Ref("validatorRef") readonly validatorRef!: any;

        get user() {
            return this.$store.state.user;
        }

        @PropSync("form")
        variable: VariableCreationDTO;

        reset() {
            this.useTraitUri = true;
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }

        isTraitUriFilled(): boolean {
            return this.variable.traitName != undefined && this.variable.traitName.length > 0;
        }

        isTraitNameFilled(): boolean {
            return this.variable.traitUri != undefined && this.variable.traitUri.length > 0;
        }
    }
</script>
<style scoped lang="scss">
</style>
