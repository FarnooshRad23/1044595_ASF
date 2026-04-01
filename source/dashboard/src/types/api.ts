// Blueprint for map coordinates
export interface Coordinates {
  latitude: number;
  longitude: number;
}

// Blueprint for a Sensor (from GET /api/devices/)
export interface SensorSummary {
  id: string;
  name: string;
  category: string;
  region: string;
  coordinates: Coordinates;
  measurement_unit: string;
  sampling_rate_hz: number;
  websocket_url: string;
}

// Blueprint for live WebSocket data (from WS /api/device/{id}/ws)
export interface SensorMeasurement {
  timestamp: string;
  value: number;
}

// Blueprint for a Detected Threat/Event (from your Database/Gateway)
export interface ActiveSensorEvent {
  eventId: string;
  eventType: string; // e.g., 'earthquake', 'conventional_explosion', 'nuclear_like'
  frequencyHz: number;
  amplitude: number;
  durationSeconds: number;
  startsAt: string;
  endsAt: string;
  label: string | null;
}

// Blueprint for the System Health (from GET /health)
export interface HealthResponse {
  status: string;
  measurementUnit: string;
  samplingRateHz: number;
  totalSensors: number;
  datacenterSensors: number;
  autoShutdownEnabled: boolean;
  autoShutdownMinSeconds: number;
  autoShutdownMaxSeconds: number;
  controlStreamConnections: number;
  generatedAt: string;
}