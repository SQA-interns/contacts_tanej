import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import { setupI18n } from './lib/i18n';
import './styles/index.css';

const i18n = setupI18n();
document.documentElement.lang = i18n.language;
document.title = i18n.t('app.name');

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
