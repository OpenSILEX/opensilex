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
      placeholder: "Number and street name"
    locality:
      label: "City"
      placeholder: "City"
    region:
      label: "Region"
      placeholder: "State or region"
    postalCode:
      label: "Postal code"
      placeholder: "Zip code"
    countryName:
      label: "Country"
      placeholder: "Country name"
fr:
  AddressForm:
    streetAddress:
      label: "Adresse"
      placeholder: "Numéro et nom de rue"
    locality:
      label: "Ville"
      placeholder: "Ville"
    region:
      label: "Region"
      placeholder: "Région ou état"
    postalCode:
      label: "Code postal"
      placeholder: "Code postal"
    countryName:
      label: "Pays"
      placeholder: "Nom du pays"
</i18n>