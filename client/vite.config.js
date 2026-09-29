import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
import { env as processEnv } from 'node:process';

export default defineConfig(({ mode }) => {
  // Load env file from the project root (parent directory)
  const env = {
    ...processEnv,
    ...loadEnv(mode, fileURLToPath(new URL('..', import.meta.url)), ''),
  };
  const defineEnv = (name) => JSON.stringify(env[name] ?? '');

  return {
    plugins: [react()],
    define: {
      // Explicitly expose VITE_ prefixed env vars to the client
      'import.meta.env.VITE_AUTH0_DOMAIN': defineEnv('VITE_AUTH0_DOMAIN'),
      'import.meta.env.VITE_AUTH0_CLIENT_ID': defineEnv('VITE_AUTH0_CLIENT_ID'),
      'import.meta.env.VITE_AUTH0_AUDIENCE': defineEnv('VITE_AUTH0_AUDIENCE'),
      'import.meta.env.VITE_API_URL': defineEnv('VITE_API_URL'),
      'import.meta.env.VITE_WS_URL': defineEnv('VITE_WS_URL'),
      'import.meta.env.VITE_DEMO_MODE': defineEnv('VITE_DEMO_MODE'),
      'import.meta.env.VITE_AUTH_MODE': defineEnv('VITE_AUTH_MODE'),
    },
    server: {
      port: 3000,
      proxy: {
        // forward any call that starts with these prefixes to the gateway
        '/user': 'http://localhost:80',
        '/location': 'http://localhost:80',
        '/messaging': 'http://localhost:80',
        '/matching': 'http://localhost:80',
        '/genai': 'http://localhost:80',
        '/ws': {      // WebSocket/STOMP endpoint
          target: 'ws://localhost:80',
          ws: true,
        },
      },
    },
  };
});