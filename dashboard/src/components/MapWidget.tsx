// src/components/MapWidget.tsx
import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet';
import type { LatLngBoundsExpression } from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Import your API client and the TypeScript blueprint you made
import apiClient from '../api/client';
import type { SensorSummary } from '../types/api';

export default function MapWidget() {
  // State to hold the sensors we get from the backend
  const [sensors, setSensors] = useState<SensorSummary[]>([]);

  const defaultCenter: [number, number] = [20.0, 0.0]; 
  const worldBounds: LatLngBoundsExpression = [[-90, -180], [90, 180]];

  // 1. Fetch the sensors when the dashboard loads
  useEffect(() => {
    const fetchSensors = async () => {
      try {
        const response = await apiClient.get('/api/devices/');
        setSensors(response.data);
      } catch (error) {
        console.error("Failed to fetch sensors:", error);
      }
    };

    fetchSensors();

    // The resize trick to fix the map window
    setTimeout(() => {
      window.dispatchEvent(new Event('resize'));
    }, 200);
  }, []);

  return (
    <div style={{ flex: 1, minHeight: '450px', maxHeight: '600px', width: '100%', borderRadius: '8px', overflow: 'hidden' }}>
      <MapContainer 
        center={defaultCenter} 
        zoom={2} 
        minZoom={2} 
        maxBounds={worldBounds} 
        maxBoundsViscosity={1.0} 
        style={{ height: '100%', width: '100%', backgroundColor: '#242424' }}
      >
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          attribution='&copy; OpenStreetMap contributors &copy; CARTO'
          noWrap={true} 
        />
        
        {/* 2. Loop through the sensors and draw a Circle for each one */}
        {sensors.map((sensor) => (
          <CircleMarker 
            key={sensor.id}
            center={[sensor.coordinates.latitude, sensor.coordinates.longitude]}
            radius={6} // Size of the dot
            color="#4caf50" // Glowing green border
            fillColor="#4caf50" // Green inside
            fillOpacity={0.7}
          >
            {/* 3. The Popup that appears on click */}
            <Popup>
              <div style={{ color: '#333', fontSize: '14px' }}>
                <strong style={{ display: 'block', fontSize: '16px', borderBottom: '1px solid #ccc', marginBottom: '5px' }}>
                  {sensor.name}
                </strong>
                <b>ID:</b> {sensor.id} <br/>
                <b>Region:</b> {sensor.region} <br/>
                <b>Category:</b> {sensor.category.toUpperCase()} <br/>
                <b>Sample Rate:</b> {sensor.sampling_rate_hz} Hz
              </div>
            </Popup>
          </CircleMarker>
        ))}

      </MapContainer>
    </div>
  );
}