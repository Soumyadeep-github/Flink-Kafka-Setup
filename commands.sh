helm delete streaming-platform
helm install streaming-platform ./helm-chart -f ./helm-chart/values-prod.yaml
kubectl get pods -n kafka-k8s
kubectl get svc -n kafka-k8s kafka-external
kubectl port-forward -n kafka-k8s svc/kafka-external 9093:9092
# Create user_activities topic
kafka-topics.sh --create \
  --topic user_activities \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:30092

# Create transactions topic
kafka-topics.sh --create \
  --topic transactions \
  --partitions 3 \
  --replication-factor 3 \
  --bootstrap-server localhost:30092

kubectl exec -it kafka-0 -n kafka-k8s -- bash

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