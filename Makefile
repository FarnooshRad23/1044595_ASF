.PHONY: up down logs

up:
	docker load -i simulator/seismic-signal-simulator-oci.tar
# 	docker run --rm -p 8080:8080 seismic-signal-simulator:multiarch_v1
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f
