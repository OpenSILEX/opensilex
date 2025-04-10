<template>
  <div class="toast-container position-fixed top-0 start-50 translate-middle-x p-3" style="z-index: 1080;">
    <div v-for="toast in toasts" :key="toast.id" class="toast show align-items-center text-white border-0 mb-2"
         :class="variantClass(toast.variant)"
         role="alert" aria-live="assertive" aria-atomic="true">
      <div class="d-flex">
        <div class="toast-body">{{ toast.message }}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto"
                @click="removeToast(toast.id)" aria-label="Close"></button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface Toast {
  id: string;
  message: string;
  variant: string;
  delay: number;
}

const toasts = ref<Toast[]>([]);

function addToast(message: string, variant = 'info', delay = 3000) {
  const id = Date.now().toString() + Math.random().toString(36).substring(2);
  toasts.value.push({ id, message, variant, delay });

  setTimeout(() => removeToast(id), delay);
}

function removeToast(id: string) {
  toasts.value = toasts.value.filter(toast => toast.id !== id);
}

function variantClass(variant: string) {
  return `bg-${variant}`;
}

// Export functions to be accessible globally
defineExpose({ addToast });
</script>

<style scoped>
/* .toast-container {
  z-index: 1080;
  width: max-content;
  max-width: 90vw;
} */

/* .toast {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  font-size: 0.95rem;
  backdrop-filter: blur(2px);
} */

/* Success */
.bg-success {
   background-color: #E6F5E9  !important;
     color: #3D7C4C !important;
}
/* .bg-success.custom-toast {
  background-color: #48c78e !important; 
  color: #fff !important;
} */

/*  Echec */
.bg-danger.custom-toast {
  background-color: #FCEDEE !important; 
}

/* Info */
.bg-info.custom-toast {
  background-color: #386f94 !important;
}

/* Warning */
.bg-warning.custom-toast {
  background-color: #c48e37 !important;
  color: #212529 !important;
}
</style>
