import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import App from './App.jsx';
import './App.css';

// Two routes, both rendering App: "/" (whatever project/tab was last active)
// and "/:itemKey" (e.g. "/SPAI-1"), which App resolves on mount to open that
// work item's detail view — see App's use of useParams()/useNavigate(). This
// is what lets a work item be linked to directly (shared URL, or a "$SPAI-1"
// reference inserted from the rich text editor) without a page reload.
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/:itemKey" element={<App />} />
        <Route path="/" element={<App />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
