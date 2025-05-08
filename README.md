# Flink-Kafka-Setup

A comprehensive setup for Apache Flink and Kafka integration with Kubernetes deployment support. This project demonstrates real-time data processing using Flink with Kafka as the message broker, deployed on Kubernetes using Helm charts.

## Project Structure
```
.
├── helm-chart/ 
├── src/ 
│ ├── main/java/ 
│ └── main/python/data_generator
├── terraform/ 
├── pom.xml 
├── requirements.txt 
└── commands.sh # Utility commands
```


## Prerequisites

- Java 11
- Maven
- Python 3.x
- Kubernetes cluster
- Helm 3.x
- Terraform (optional, for infrastructure setup)

## Dependencies

### Java Dependencies
- Apache Flink 1.18.1
- Flink Kafka Connector 1.17.2
- Flink State Backend (RocksDB)
- Flink JSON Processing
- SLF4J for logging
- Lombok

### Python Dependencies
- kafka-python
- kafka-python-ng
- faker

## Setup Instructions

1. **Install Dependencies**
   ```bash
   # Install Java dependencies
   mvn clean install

   # Install Python dependencies
   pip install -r requirements.txt
   ```

2. **Kubernetes Deployment**
   ```bash
   # Deploy using Helm
   helm install streaming-platform ./helm-chart
   ```

3. **Verify Deployment**
   ```bash
   # Check Kafka service
   kubectl get svc -n kafka-k8s kafka-external

   # Check Flink deployment
   kubectl get pods -n kafka-k8s -l app=flink-jobmanager
   ```
4. **Post Deployment**
   ```bash
   # Create user_activities topic
    kubectl exec -it kafka-0 -n kafka-k8s -- /bin/kafka-topics --create \
    --topic user_activities \
    --partitions 3 \
    --replication-factor 3 \
    --bootstrap-server localhost:9092

   # Create transactions topic
    kubectl exec -it kafka-0 -n kafka-k8s -- /bin/kafka-topics --create \
    --topic transactions \
    --partitions 3 \
    --replication-factor 3 \
    --bootstrap-server localhost:9092

   # Exposes the broker to external traffic
   kubectl port-forward -n kafka-k8s svc/kafka-external 9093:9092

   # Exposes the Flink UI
   kubectl port-forward -n kafka-k8s svc/flink-jobmanager 8082:8081
   ```

## Features

- Real-time data processing with Apache Flink
- Kafka integration for message streaming
- RocksDB state backend for state management
- Kubernetes deployment support
- Helm charts for easy deployment
- Terraform infrastructure as code
- Python-based data generation utilities

## Architecture

The system consists of:
- Kafka brokers for message streaming
- Flink job manager and task managers for data processing
- RocksDB for state management
- Kubernetes for container orchestration

## Development

1. **Building the Project**
   ```bash
   mvn clean package
   ```

2. **Running Tests**
   ```bash
   mvn test
   ```

3. **Generating Test Data**
   ```bash
   python src/main/python/data_generator.py
   ```

4. **Flink Job Submission**
   ```bash
   flink run -m localhost:8082 -c org.example.StreamingAnalyticsJob  target/Flink-Kafka-Helm-1.0-SNAPSHOT.jar
   ```

## Monitoring

- Flink Web UI: Access through port-forwarding
- Kafka Web UI: Access through port-forwarding


