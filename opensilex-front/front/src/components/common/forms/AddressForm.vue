<template>
  <div v-if="addressForm">
    <!-- Street address -->
    <opensilex-InputForm
        :value.sync="addressForm.streetAddress"
        label="AddressForm.streetAddress.label"
        type="text"
        placeholder="AddressForm.streetAddress.placeholder"
        @input="onAddressInput"
        @blur="onAddressBlur"
    ></opensilex-InputForm>

    <!-- Autocomplete -->
    <b-button-group
        vertical
        ref="autocompleteMenu"
        class="autocompleteMenu"
    >
      <b-button
          v-for="address in autocompleteAddressList"
          :key="address.readableAddress"
          @click="() => onAddressAutocompleteClick(address)"
          class="js-autocompleteButton"
      >
        {{ address.readableAddress }}
      </b-button>
    </b-button-group>

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
import {Component, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import IGeocodingService, {GeocodingAddressResult} from "../../../services/geocoding/IGeocodingService";
import { FacilityAddressDTO } from 'opensilex-core/index';

const AUTOCOMPLETE_TIMEOUT_MS: number = 250;

@Component
export default class AddressForm extends Vue {
  $opensilex: OpenSilexVuePlugin;

  geocodingService: IGeocodingService;

  @PropSync("address")
  addressForm: FacilityAddressDTO;

  @Ref("autocompleteMenu")
  autocompleteMenu: Element;
  autocompleteAddressList: Array<GeocodingAddressResult> = [];

  /**
   * Timeout until the "address input pause" triggers
   */
  addressInputTimeout: ReturnType<typeof setTimeout>;

  created() {
    this.geocodingService = this.$opensilex.getService("IGeocodingService");
  }

  mounted() {
    document.addEventListener("click", this.clearAutocompleteMenu);
  }

  beforeUnmount() {
    document.removeEventListener("click", this.clearAutocompleteMenu);
  }

  onAddressBlur() {
    clearTimeout(this.addressInputTimeout);
  }

  clearAutocompleteMenu() {
    this.autocompleteAddressList = [];
  }

  /**
   * When the address value changes, triggers a timeout for 1s. If the value doesn't change and the input doesn't lose
   * focus during this timeout , then the "addressInputPause" method is called to provide lookahead suggestions.
   */
  onAddressInput() {
    clearTimeout(this.addressInputTimeout);
    this.addressInputTimeout = setTimeout(() => {
      this.onAddressInputPause(this.addressForm.streetAddress);
    }, AUTOCOMPLETE_TIMEOUT_MS);
  }

  onAddressInputPause(partialAddress: string) {
    let lang = this.$opensilex.getLang();
    this.geocodingService.search(partialAddress, {
      lang
    }).then((addresses) => {
      this.autocompleteAddressList = addresses;
    }, (error) => {
      console.warn("Error while fetching address suggestions", error);
    })
  }

  onAddressAutocompleteClick(address: GeocodingAddressResult) {
    console.log("Autocomplete click", address);

    this.addressForm.countryName = address.country;
    this.addressForm.postalCode = address.postcode;
    this.addressForm.region = address.state ? address.state : address.county;
    this.addressForm.locality = address.city;

    let streetAddress = "";
    if (address.street && address.street.length > 0) {
      if (address.houseNumber && address.houseNumber.length > 0) {
        streetAddress += `${address.houseNumber}, `;
      }
      streetAddress += address.street;
    } else if (address.name) {
      streetAddress = address.name;
    }
    this.addressForm.streetAddress = streetAddress;

    this.autocompleteAddressList = [];
  }
}
</script>

<style scoped lang="scss">
.autocompleteMenu {
  position: absolute;
  z-index: 1;
}
</style>

<i18n>
en:
  AddressForm:
    streetAddress:
      label: "Address"
      placeholder: "Number and street name (start typing to see suggestions...)"
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
      placeholder: "Numéro et nom de rue (commencer à taper pour voir des suggestions...)"
    locality:
      label: "Ville"
      placeholder: "Ville"
    region:
      label: "Région"
      placeholder: "Région ou état"
    postalCode:
      label: "Code postal"
      placeholder: "Code postal"
    countryName:
      label: "Pays"
      placeholder: "Nom du pays"
</i18n>