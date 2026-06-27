<script setup lang="ts">
import { ref } from 'vue'

const transitionName = ref('fade-slide')
</script>

<template>
  <router-view v-slot="{ Component, route }">
    <Transition
      :name="transitionName"
      mode="out-in"
    >
      <KeepAlive :max="10">
        <component :is="Component" :key="route.path" />
      </KeepAlive>
    </Transition>
  </router-view>
</template>

<style lang="scss">
#app {
  width: 100%;
  height: 100%;
}

/* 路由过渡动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 缩放动画 */
.scale-enter-active,
.scale-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.scale-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.scale-leave-to {
  opacity: 0;
  transform: scale(1.05);
}
</style>
