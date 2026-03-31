.PHONY: up down logs

up:
	docker load -i simulator/seismic-signal-simulator-oci.tar
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f
