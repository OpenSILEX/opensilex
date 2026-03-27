<template>
  <opensilex-StringView :label="label">
    <div class="geometry-copy">
      <span
        class="badge bg-secondary me-2 copy"
        :title="t('component.copyGeometryWKT')"
        @click.prevent.stop="copyGeometry(wktValue(value), 'WKT')"
      >
        {{ t('component.geometryWKT') }}
        <opensilex-Icon icon="ik#ik-copy" />
      </span>

      <span
        class="badge bg-secondary copy"
        :title="t('component.copyGeometryGeoJSON')"
        @click.prevent.stop="copyGeometry(JSON.geojsonToWKT(value), 'GeoJSON')"
      >
        {{ t('component.geometryGeoJSON') }}
        <opensilex-Icon icon="ik#ik-copy" />
      </span>
    </div>
  </opensilex-StringView>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import { useI18n } from 'vue-i18n'
// import { stringify } from 'wkt'
import { wktToGeoJSON, geojsonToWKT } from '@terraformer/wkt'
import copy from 'copy-to-clipboard'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const props = withDefaults(
  defineProps<{
    value: any
    label?: string
  }>(),
  {
    label: 'component.geometry'
  }
)

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

function wktValue(geometry: any): string {
  try {
    return geojsonToWKT(geometry)
  } catch {
    return ''
  }
}

function copyGeometry(value: string, standard: 'WKT' | 'GeoJSON') {
  if (!value) return

  copy(value)

  $opensilex.showSuccessToast(
    standard === 'GeoJSON'
      ? t('component.copiedGeometryGeoJSON')
      : t('component.copiedGeometryWKT')
  )
}
</script>

<style scoped lang="scss">
.copy {
  cursor: copy;
  user-select: none;
}
</style>

<i18n>
en:
  component:
    geometry: Geometry
    copyGeometryGeoJSON: Copy geospatial coordinates to GeoJSON format
    copiedGeometryGeoJSON: Geospatial coordinates have been copied to GeoJSON format.
    geometryGeoJSON: GeoJSON
    copyGeometryWKT: Copy geospatial coordinates to WKT format
    copiedGeometryWKT: Geospatial coordinates have been copied to WKT format.
    geometryWKT: WKT
fr:
  component:
    geometry: Géométrie
    copyGeometryGeoJSON: Copier les coordonnées géospatiales au format GeoJSON
    copiedGeometryGeoJSON: Les coordonnées géospatiales ont été copiées au format GeoJSON.
    geometryGeoJSON: GeoJSON
    copyGeometryWKT: Copier les coordonnées géospatiales au format WKT
    copiedGeometryWKT: Les coordonnées géospatiales ont été copiées au format WKT.
    geometryWKT: WKT
</i18n>
