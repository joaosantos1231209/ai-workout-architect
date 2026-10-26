// Ambiente de Desenvolvimento & Produção Automático
const isBrowser = typeof window !== 'undefined';
const hostname = isBrowser ? window.location.hostname : 'localhost';

const isLocal =
  hostname === 'localhost' ||
  hostname === '127.0.0.1' ||
  hostname.startsWith('192.168.') ||
  hostname.startsWith('10.');

export const environment = {
  production: !isLocal,
  version: 'v1.6.0',
  // Se estiver a correr localmente ou na rede Wi-Fi de casa, usa a máquina local.
  // Se estiver na Vercel (*.vercel.app), aponta para o backend alojado na cloud.
  apiUrl: isLocal
    ? `http://${hostname}:8080/api/v1`
    : 'https://ai-workout-architect.onrender.com/api/v1'
};
