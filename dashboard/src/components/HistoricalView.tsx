// src/components/HistoricalView.tsx
import { useEffect, useMemo, useState } from 'react';

type HistoricalRow = {
  eventId: string;
  sensorId: string;
  region: string | null;
  time: string;
  type: string;
  freq: number;
  amp: number;
};

export default function HistoricalView() {
  const [rows, setRows] = useState<HistoricalRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [filterSensor, setFilterSensor] = useState('');
  const [filterRegion, setFilterRegion] = useState('');
  const [filterType, setFilterType] = useState('');
  const [filterDate, setFilterDate] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    async function loadEvents() {
      try {
        setLoading(true);
        setError(null);

        const params = new URLSearchParams();

        if (filterSensor.trim()) params.set('sensorId', filterSensor.trim());
        if (filterRegion.trim()) params.set('region', filterRegion.trim());
        if (filterType.trim()) params.set('type', filterType.trim());
        if (filterDate.trim()) params.set('date', filterDate.trim());

        params.set('limit', '200');
        params.set('offset', '0');

        const response = await fetch(`/api/events?${params.toString()}`, {
          method: 'GET',
          signal: controller.signal,
          headers: {
            Accept: 'application/json',
          },
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data: HistoricalRow[] = await response.json();
        setRows(data);
      } catch (err) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        setError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        setLoading(false);
      }
    }

    loadEvents();

    return () => controller.abort();
  }, [filterSensor, filterRegion, filterType, filterDate]);

  const filteredData = useMemo(() => rows, [rows]);

  const getThreatColor = (type: string) => {
    if (type === 'nuclear_like') return '#d32f2f';
    if (type === 'conventional_explosion') return '#f57c00';
    if (type === 'earthquake') return '#fbc02d';
    return '#888888';
  };

  return (
    <div
      style={{
        flex: 1,
        backgroundColor: '#242424',
        borderRadius: '8px',
        padding: '20px',
        color: '#ffffff',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
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
                  style={{
                    width: '100%',
                    padding: '5px',
                    backgroundColor: '#333',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '4px',
                  }}
                />
              </th>

              <th style={{ padding: '5px 10px 15px 10px' }}>
                <select
                  value={filterRegion}
                  onChange={(e) => setFilterRegion(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '5px',
                    backgroundColor: '#333',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '4px',
                  }}
                >
                  <option value="">All Regions</option>
                  <option value="Replica Datacenter">Replica Datacenter</option>
                  <option value="Field Station Alpha">Field Station Alpha</option>
                  <option value="Field Station Bravo">Field Station Bravo</option>
                </select>
              </th>

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
                    colorScheme: 'dark',
                  }}
                />
              </th>

              <th style={{ padding: '5px 10px 15px 10px' }}>
                <select
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                  style={{
                    width: '100%',
                    padding: '5px',
                    backgroundColor: '#333',
                    color: '#fff',
                    border: 'none',
                    borderRadius: '4px',
                  }}
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
            {loading ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '20px', color: '#888' }}>
                  Loading historical data...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '20px', color: '#ff6b6b' }}>
                  Failed to load data: {error}
                </td>
              </tr>
            ) : filteredData.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '20px', color: '#888' }}>
                  No historical data matches your filters.
                </td>
              </tr>
            ) : (
              filteredData.map((row) => (
                <tr
                  key={row.eventId}
                  style={{
                    borderBottom: '1px solid #333',
                    transition: 'background-color 0.2s',
                  }}
                >
                  <td style={{ padding: '12px 10px' }}>{row.sensorId}</td>
                  <td style={{ padding: '12px 10px' }}>{row.region ?? 'Unknown'}</td>
                  <td style={{ padding: '12px 10px' }}>
                    {new Date(row.time).toLocaleDateString()}{' '}
                    {new Date(row.time).toLocaleTimeString([], { hour12: false })}
                  </td>
                  <td
                    style={{
                      padding: '12px 10px',
                      color: getThreatColor(row.type),
                      fontWeight: 'bold',
                    }}
                  >
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