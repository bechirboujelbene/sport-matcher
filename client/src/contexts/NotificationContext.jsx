import React, { createContext, useCallback, useEffect, useRef, useState } from 'react';
import Notification from '../components/Notification';

const NotificationContext = createContext({ /* default */ });

let idCounter = 0;

export const NotificationProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef({});

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    if (timersRef.current[id]) {
      clearTimeout(timersRef.current[id]);
      delete timersRef.current[id];
    }
  }, []);

  const notify = useCallback(({
    type = 'info',
    message = '',
    duration = 4000,
  }) => {
    const id = ++idCounter;
    setToasts((prev) => [...prev, { id, type, message }]);
    timersRef.current[id] = setTimeout(() => removeToast(id), duration);
  }, [removeToast]);

  // Clean up on unmount
  useEffect(() => () => {
    Object.values(timersRef.current).forEach(clearTimeout);
  }, []);

  return (
    <NotificationContext.Provider value={{ notify }}>
      {children}
      {/* Toast container */}
      <div className="notification-container">
        {toasts.map((toast) => (
          <Notification
            key={toast.id}
            type={toast.type}
            message={toast.message}
            onClose={() => removeToast(toast.id)}
          />
        ))}
      </div>
    </NotificationContext.Provider>
  );
};

export default NotificationContext;
