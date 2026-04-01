
# Simulator: how to start

## load and run it
```bash
cd source/simulator
docker load -i seismic-signal-simulator-oci.tar
```
This registers the image seismic-signal-simulator:multiarch_v1 in your local Docker. (remember to enable "Use containerd for pulling and storing images" into docker desktop settings)
Then run it.
```bash
docker run --rm -p 8080:8080 seismic-signal-simulator:multiarch_v1
```

## Verify it's working:
```bash
curl -s http://localhost:8080/health | jq
```

it should return something like:
```json
{
  "status": "ok",
  "measurementUnit": "mm/s",
  "samplingRateHz": 20.0,
  "totalSensors": 12,
  "datacenterSensors": 5,
  "autoShutdownEnabled": true,
  "autoShutdownMinSeconds": 30.0,
  "autoShutdownMaxSeconds": 90.0,
  "controlStreamConnections": 0,
  "generatedAt": "2026-03-29T19:49:51.911254+00:00"
}
```

## Verify sensor discovery:
```bash
curl -s http://localhost:8080/api/devices/ | jq
```
You should get a JSON array of 12 sensors.

## Verify a WebSocket stream

Install websocat (a command-line WebSocket client) or use any WS tool:
```bash
# install websocat (macOS)
brew install websocat
```
```bash
# connect to one sensor
websocat ws://localhost:8080/api/device/sensor-01/ws
```
You should see a stream of JSON messages arriving every ~50ms.

## Verify the control SSE stream
```bash
curl -N http://localhost:8080/api/control
```

You should see SSE events:
```
event: control-open
data: {"connectedAt":"...","controlStreamConnections":1}

event: heartbeat
data: {"timestamp":"...","controlStreamConnections":1}
```

And eventually (within 30-90 seconds) a shutdown command:
```
event: command
data: {"command":"SHUTDOWN"}

```

## Verify manual event injection
```bash
curl -X POST http://localhost:8080/api/admin/sensors/sensor-01/events \
  -H "Content-Type: application/json" \
  -d '{"event_type":"earthquake"}'
```
If you have websocat still connected to sensor-01, you should notice the signal values change dramatically for a few seconds (the simulator injects a 1.6 Hz wave). This is how you'll test your FFT later.

---

Fast tests:
```bash
docker run --rm -p 8080:8080 seismic-signal-simulator:multiarch_v1

curl -s http://localhost:8080/health | jq .status
# expect: "ok"

curl -s http://localhost:8080/api/devices/ | jq length
# expect: 12

websocat ws://localhost:8080/api/device/sensor-01/ws | head -3
# expect: 3 lines of {"timestamp":"...","value":...}
```