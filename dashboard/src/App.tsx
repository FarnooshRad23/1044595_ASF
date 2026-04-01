import { useState } from 'react';
import SystemHealth from './components/SystemHealth';
import RealtimeFeed from './components/RealtimeFeed';
import MapWidget from './components/MapWidget';
import HistoricalView from './components/HistoricalView'; // 1. Import the new page

function App() {
  // 2. State to track which page is active ('map' or 'history')
  const [activePage, setActivePage] = useState<'map' | 'history'>('map');

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', backgroundColor: '#18181b', color: '#ffffff' }}>
      
      <SystemHealth />

      {/* 3. Navigation Tabs */}
      <div style={{ display: 'flex', padding: '0 20px', gap: '10px', marginBottom: '10px' }}>
        <button 
          onClick={() => setActivePage('map')}
          style={{
            padding: '8px 16px',
            backgroundColor: activePage === 'map' ? '#4caf50' : '#333',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold'
          }}
        >
          LIVE MAP VIEW
        </button>
        <button 
          onClick={() => setActivePage('history')}
          style={{
            padding: '8px 16px',
            backgroundColor: activePage === 'history' ? '#4caf50' : '#333',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold'
          }}
        >
          HISTORICAL DATA
        </button>
      </div>

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        
        {/* Left Side: Conditional Rendering based on the active tab */}
        <div style={{ flex: 1, padding: '0 20px 20px 20px', display: 'flex', flexDirection: 'column' }}>
          {activePage === 'map' ? <MapWidget /> : <HistoricalView />}
        </div>

        {/* Right Side: Live Alerts Feed (Always visible!) */}
        <div style={{ borderLeft: '1px solid #333' }}>
          <RealtimeFeed />
        </div>
        
      </div>
    </div>
  );
}

export default App;