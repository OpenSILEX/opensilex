<template>
  <div v-if="addressState">
    <!-- Street address -->
    <opensilex-InputForm
      v-model:value="addressState.streetAddress"
      :label="t('AddressForm.streetAddress.label')"
      type="text"
      :placeholder="t('AddressForm.streetAddress.placeholder')"
      @input="onAddressInput"
      @blur="onAddressBlur"
    />

    <!-- Autocomplete -->
    <div ref="autocompleteMenuRef" class="autocompleteMenu btn-group-vertical w-100">
      <button
        v-for="address in autocompleteAddressList"
        :key="address.readableAddress"
        type="button"
        class="btn btn-light text-start js-autocompleteButton"
        @click.prevent.stop="onAddressAutocompleteClick(address)"
      >
        {{ address.readableAddress }}
      </button>
    </div>

    <!-- Locality -->
    <opensilex-InputForm
      v-model:value="addressState.locality"
      :label="t('AddressForm.locality.label')"
      type="text"
      :placeholder="t('AddressForm.locality.placeholder')"
    />

    <div class="row">
      <div class="col-12 col-lg-8">
        <!-- Region -->
        <opensilex-InputForm
          v-model:value="addressState.region"
          :label="t('AddressForm.region.label')"
          type="text"
          :placeholder="t('AddressForm.region.placeholder')"
        />
      </div>

      <div class="col-12 col-lg-4">
        <!-- Postal code -->
        <opensilex-InputForm
          v-model:value="addressState.postalCode"
          :label="t('AddressForm.postalCode.label')"
          type="text"
          :placeholder="t('AddressForm.postalCode.placeholder')"
        />
      </div>
    </div>

    <!-- Country -->
    <opensilex-InputForm
      v-model:value="addressState.countryName"
      :label="t('AddressForm.countryName.label')"
      type="text"
      :placeholder="t('AddressForm.countryName.placeholder')"
    />
  </div>
</template>

<script setup lang="ts">
import { inject, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type IGeocodingService from "../../../services/geocoding/IGeocodingService";
import type { GeocodingAddressResult } from "../../../services/geocoding/IGeocodingService";
import type { FacilityAddressDTO } from 'opensilex-core/index'

const { t } = useI18n()
const AUTOCOMPLETE_TIMEOUT_MS = 250

const props = defineProps<{
  address: FacilityAddressDTO
}>()

const emit = defineEmits<{
  (e: 'update:address', v: FacilityAddressDTO): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const geocodingService = $opensilex.getService<IGeocodingService>('IGeocodingService')

const addressState = ref<FacilityAddressDTO>(props.address)

watch(
  () => props.address,
  (v) => {
    addressState.value = v
  },
  { deep: true }
)

watch(
  addressState,
  (v) => emit('update:address', v),
  { deep: true }
)

const autocompleteMenuRef = ref<HTMLElement | null>(null)
const autocompleteAddressList = ref<GeocodingAddressResult[]>([])
let addressInputTimeout: ReturnType<typeof setTimeout> | undefined

function clearAutocompleteMenu() {
  autocompleteAddressList.value = []
}

function onDocumentClick(e: MouseEvent) {
  // Si click dans le menu autocomplete -> ne pas fermer
  const menu = autocompleteMenuRef.value
  if (menu && e.target && menu.contains(e.target as Node)) return
  clearAutocompleteMenu()
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  if (addressInputTimeout) clearTimeout(addressInputTimeout)
})

function onAddressBlur() {
  if (addressInputTimeout) clearTimeout(addressInputTimeout)
}

function onAddressInput() {
  if (addressInputTimeout) clearTimeout(addressInputTimeout)
  addressInputTimeout = setTimeout(() => {
    onAddressInputPause(addressState.value?.streetAddress)
  }, AUTOCOMPLETE_TIMEOUT_MS)
}

async function onAddressInputPause(partialAddress?: string) {
  if (!partialAddress || !partialAddress.trim()) {
    autocompleteAddressList.value = []
    return
  }
  const lang = $opensilex.getLang()
  try {
    const addresses = await geocodingService.search(partialAddress, { lang })
    autocompleteAddressList.value = addresses ?? []
  } catch (e) {
    console.warn('Error while fetching address suggestions', e)
  }
}

function onAddressAutocompleteClick(address: GeocodingAddressResult) {
  if (!addressState.value) return

  addressState.value.countryName = address.country
  addressState.value.postalCode = address.postcode
  addressState.value.region = address.state ? address.state : address.county
  addressState.value.locality = address.city

  let streetAddress = ''
  if (address.street && address.street.length > 0) {
    if (address.houseNumber && address.houseNumber.length > 0) {
      streetAddress += `${address.houseNumber}, `
    }
    streetAddress += address.street
  } else if ((address as any).name) {
    streetAddress = (address as any).name
  }

  addressState.value.streetAddress = streetAddress
  autocompleteAddressList.value = []
}
</script>

<style scoped lang="scss">
.autocompleteMenu {
  position: absolute;
  z-index: 1000;
  max-height: 240px;
  overflow: auto;
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
