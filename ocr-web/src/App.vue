<script setup lang="ts">
</script>

<template>
  <router-view />
</template>

<style>
:root {
  --primary: #4f6ef7;
  --primary-light: #e8ecfe;
  --success: #34c759;
  --warning: #ff9500;
  --danger: #ff3b30;
  --bg: #f5f6fa;
  --card: #ffffff;
  --text: #1a1a2e;
  --text-secondary: #6b7280;
  --border: #e5e7eb;
  --radius: 12px;
  --radius-sm: 8px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--text);
  -webkit-font-smoothing: antialiased;
  -webkit-tap-highlight-color: transparent;
}

button {
  font-family: inherit;
  cursor: pointer;
  border: none;
  outline: none;
}

input, textarea {
  font-family: inherit;
  outline: none;
}
</style>
