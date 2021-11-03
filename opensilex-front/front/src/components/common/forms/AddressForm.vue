<template>
  <div v-if="addressForm">
    <!-- Street address -->
    <opensilex-InputForm
        :value.sync="addressForm.streetAddress"
        label="AddressForm.streetAddress.label"
        type="text"
        placeholder="AddressForm.streetAddress.placeholder"
    ></opensilex-InputForm>

    <!-- Locality -->
    <opensilex-InputForm
        :value.sync="addressForm.locality"
        label="AddressForm.locality.label"
        type="text"
        placeholder="AddressForm.locality.placeholder"
    ></opensilex-InputForm>

    <b-row>
      <b-col cols="8">
        <!-- Region -->
        <opensilex-InputForm
            :value.sync="addressForm.region"
            label="AddressForm.region.label"
            type="text"
            placeholder="AddressForm.region.placeholder"
        ></opensilex-InputForm>
      </b-col>
      <b-col cols="4">
        <!-- Postal code -->
        <opensilex-InputForm
            :value.sync="addressForm.postalCode"
            label="AddressForm.postalCode.label"
            type="text"
            placeholder="AddressForm.postalCode.placeholder"
        ></opensilex-InputForm>
      </b-col>
    </b-row>

    <!-- Country -->
    <opensilex-InputForm
        :value.sync="addressForm.countryName"
        label="AddressForm.countryName.label"
        type="text"
        placeholder="AddressForm.countryName.placeholder"
    ></opensilex-InputForm>
  </div>
</template>

<script lang="ts">
import {Component, PropSync} from "vue-property-decorator";
import Vue from "vue";
import {FacilityAddressDTO} from "opensilex-core/model/facilityAddressDTO";

@Component
export default class AddressForm extends Vue {
  @PropSync("address")
  addressForm: FacilityAddressDTO;


}
</script>

<style scoped>

</style>

<i18n>
en:
  AddressForm:
    streetAddress:
      label: "Address"
      placeholder: "55 Faubourg Saint-Honoré St."
    locality:
      label: "City"
      placeholder: "Paris"
    region:
      label: "Region"
      placeholder: "Île-de-France"
    postalCode:
      label: "Postal code"
      placeholder: "75008"
    countryName:
      label: "Country"
      placeholder: "France"
fr:
  AddressForm:
    streetAddress:
      label: "Adresse"
      placeholder: "55 rue du Faubourg Saint-Honoré"
    locality:
      label: "Ville"
      placeholder: "Paris"
    region:
      label: "Region"
      placeholder: "Île-de-France"
    postalCode:
      label: "Code postal"
      placeholder: "75008"
    countryName:
      label: "Pays"
      placeholder: "France"
</i18n>