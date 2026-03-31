// src/components/HistoricalView.tsx
import { useState } from 'react';

// Temporary dummy data
const MOCK_HISTORY = [
  { eventId: 'evt-001', sensorId: 'sensor-08', region: 'Replica Datacenter', time: '2026-03-31T14:32:00Z', type: 'nuclear_like', freq: 8.5, amp: 10.5 },
  { eventId: 'evt-002', sensorId: 'sensor-01', region: 'Field Station Alpha', time: '2026-03-31T13:48:12Z', type: 'conventional_explosion', freq: 5.6, amp: 10.5 },
  { eventId: 'evt-003', sensorId: 'sensor-05', region: 'Field Station Bravo', time: '2026-03-30T12:05:00Z', type: 'earthquake', freq: 1.9, amp: 2.8 },
  { eventId: 'evt-004', sensorId: 'sensor-08', region: 'Replica Datacenter', time: '2026-03-29T09:12:44Z', type: 'datacenter_shutdown_disturbance', freq: 6.5, amp: 5.0 },
];

export default function HistoricalView() {
  // State for all our column filters
  const [filterSensor, setFilterSensor] = useState('');
  const [filterRegion, setFilterRegion] = useState('');
  const [filterType, setFilterType] = useState('');
  
  // NEW: State for the calendar filter
  const [filterDate, setFilterDate] = useState('');

  // Filter the data based on ALL active filters
  const filteredData = MOCK_HISTORY.filter(row => {
    // Extract just the 'YYYY-MM-DD' part of the timestamp for the calendar comparison
    const rowDate = row.time.split('T')[0]; 

    const matchesSensor = row.sensorId.toLowerCase().includes(filterSensor.toLowerCase());
    const matchesRegion = row.region.toLowerCase().includes(filterRegion.toLowerCase());
    const matchesType = row.type.toLowerCase().includes(filterType.toLowerCase());
    // If filterDate is empty, match everything. Otherwise, match the exact date.
    const matchesDate = filterDate === '' || rowDate === filterDate;

    return matchesSensor && matchesRegion && matchesType && matchesDate;
  });

  const getThreatColor = (type: string) => {
    if (type === 'nuclear_like') return '#d32f2f'; 
    if (type === 'conventional_explosion') return '#f57c00'; 
    if (type === 'earthquake') return '#fbc02d'; 
    return '#888888'; 
  };

  return (
    <div style={{ flex: 1, backgroundColor: '#242424', borderRadius: '8px', padding: '20px', color: '#ffffff', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      
      <h2 style={{ marginTop: 0, marginBottom: '20px', color: '#fff', fontSize: '1.2rem' }}>
        HISTORICAL INTELLIGENCE DATABASE
      </h2>

      <div style={{ flex: 1, overflowY: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
          
          <thead style={{ position: 'sticky', top: 0, backgroundColor: '#242424', zIndex: 1 }}>
            <tr style={{ borderBottom: '1px solid #444', color: '#aaa' }}>
              <th style={{ padding: '10px' }}>Sensor ID</th>
              <th style={{ padding: '10px' }}>Region</th>
              <th style={{ padding: '10px' }}>Time (UTC)</th>
              <th style={{ padding: '10px' }}>Threat Classification</th>
              <th style={{ padding: '10px' }}>Freq (Hz)</th>
              <th style={{ padding: '10px' }}>Amp (mm/s)</th>
            </tr>
            <tr style={{ borderBottom: '2px solid #555' }}>
              <th style={{ padding: '5px 10px 15px 10px' }}>
                <input 
                  placeholder="Search ID..." 
                  value={filterSensor}
                  onChange={(e) => setFilterSensor(e.target.value)}
                  style={{ width: '100%', padding: '5px', backgroundColor: '#333', color: '#fff', border: 'none', borderRadius: '4px' }} 
                />
              </th>
              <th style={{ padding: '5px 10px 15px 10px' }}>
                <select 
                  value={filterRegion}
                  onChange={(e) => setFilterRegion(e.target.value)}
                  style={{ width: '100%', padding: '5px', backgroundColor: '#333', color: '#fff', border: 'none', borderRadius: '4px' }}
                >
                  <option value="">All Regions</option>
                  <option value="Replica Datacenter">Replica Datacenter</option>
                  <option value="Field Station Alpha">Field Station Alpha</option>
                  <option value="Field Station Bravo">Field Station Bravo</option>
                </select>
              </th>
              {/* NEW: Calendar Date Picker Filter */}
              <th style={{ padding: '5px 10px 15px 10px' }}>
                <input 
                  type="date"
                  value={filterDate}
                  onChange={(e) => setFilterDate(e.target.value)}
                  style={{ 
                    width: '100%', 
                    padding: '5px', 
                    backgroundColor: '#333', 
                    color: '#fff', 
                    border: 'none', 
                    borderRadius: '4px',
                    colorScheme: 'dark' // Forces the popup calendar to be dark mode in modern browsers!
                  }} 
                />
              </th>
              <th style={{ padding: '5px 10px 15px 10px' }}>
                <select 
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                  style={{ width: '100%', padding: '5px', backgroundColor: '#333', color: '#fff', border: 'none', borderRadius: '4px' }}
                >
                  <option value="">All Threats</option>
                  <option value="nuclear_like">Nuclear-Like</option>
                  <option value="conventional_explosion">Conventional Explosion</option>
                  <option value="earthquake">Earthquake</option>
                </select>
              </th>
              <th colSpan={2}></th>
            </tr>
          </thead>

          <tbody>
            {filteredData.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '20px', color: '#888' }}>
                  No historical data matches your filters.
                </td>
              </tr>
            ) : (
              filteredData.map((row) => (
                <tr key={row.eventId} style={{ borderBottom: '1px solid #333', transition: 'background-color 0.2s' }}>
                  <td style={{ padding: '12px 10px' }}>{row.sensorId}</td>
                  <td style={{ padding: '12px 10px' }}>{row.region}</td>
                  <td style={{ padding: '12px 10px' }}>
                    {/* Shows both Date and Time cleanly */}
                    {new Date(row.time).toLocaleDateString()} {new Date(row.time).toLocaleTimeString([], { hour12: false })}
                  </td>
                  <td style={{ padding: '12px 10px', color: getThreatColor(row.type), fontWeight: 'bold' }}>
                    {row.type.replace(/_/g, ' ').toUpperCase()}
                  </td>
                  <td style={{ padding: '12px 10px' }}>{row.freq.toFixed(1)}</td>
                  <td style={{ padding: '12px 10px' }}>{row.amp.toFixed(1)}</td>
                </tr>
              ))
            )}
          </tbody>

        </table>
      </div>
    </div>
  );
}