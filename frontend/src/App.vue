<script setup lang="ts">
import { ref } from "vue";

const count = ref(0);

//也可以定义函数，模板里用@click调用
function increase() {
  count.value++;
}
</script>

<template>
  <!-- {{ }} 是插值表达式，把变量的值渲染成文本 -->
  <div class="page">
    <h1>rag-demo 前端</h1>
    <p>当前技术:{{ count }}</p>
    <!-- @click 是 v-on:click 的简写，表示"点击时执行" -->
    <button @click="increase">点我+1</button>
  </div>
</template>

<style scoped>
/* scoped = 这段样式只作用于本组件，不会污染其他组件 */
.page {
  max-width: 720px;
  margin: 60px auto;
  font-family:
    system-ui,
    -apple-system,
    "Microsoft YaHei",
    sans-serif;
  text-align: center;
}
h1 {
  font-size: 24px;
}
button {
  padding: 8px 16px;
  font-size: 16px;
  cursor: pointer;
}
</style>
