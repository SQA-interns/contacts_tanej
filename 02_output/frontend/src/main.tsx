import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';
import { setupI18n } from './lib/i18n';
import { applyThemeMode, readThemeMode } from './lib/theme';
import './styles/index.css';

// the user's light or dark choice before the first paint (the default follows the system)
applyThemeMode(readThemeMode());
const i18n = setupI18n();
document.documentElement.lang = i18n.language;
document.title = i18n.t('app.name');

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
