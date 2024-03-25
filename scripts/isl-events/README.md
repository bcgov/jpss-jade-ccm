
# Docker Compose Kafka Setup

This is Docker Compose configuration for setting up a local Kafka environment with a UI.

## Services

1. **events_zookeeper**: A ZooKeeper instance for managing Kafka.
   - Image: `confluentinc/cp-zookeeper:7.3.0`
   - Port: Internal (Not exposed)
2. **events_broker**: A Kafka broker.
   - Image: `confluentinc/cp-kafka:7.3.0`
   - Port: `9092`
3. **events_ui**: A UI for Kafka based on AKHQ.
   - Image: `tchiotludo/akhq`
   - Port: `8081`

## Getting Started

1. Clone this repository.
2. Navigate to the directory containing the `docker-compose.yml` file.
3. Run the following command to start the services:
   ```
   docker-compose up -d
   ```
4. Once the services are up, you can access the Kafka UI at `http://localhost:8081`.

## Stopping the Services

To stop the services, navigate to the directory containing the `docker-compose.yml` file and run:
```
docker-compose down
```

## Notes

- Ensure that you have Docker and Docker Compose installed on your machine.
- The Kafka UI uses a configuration file `events_ui.yml`. Ensure it's present in the same directory as the `docker-compose.yml` file.
