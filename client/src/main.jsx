import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { AppAuthProvider } from './auth/AppAuthProvider.jsx';
import './index.css';
import './styles/formOverrides.css';
import { NotificationProvider } from './contexts/NotificationContext.jsx';
import './styles/Notification.css';
import App from './App.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AppAuthProvider>
      <NotificationProvider>
        <BrowserRouter basename={import.meta.env.BASE_URL}>
          <App />
        </BrowserRouter>
      </NotificationProvider>
    </AppAuthProvider>
  </StrictMode>
);
