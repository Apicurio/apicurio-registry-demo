minikube start --memory="8000m"
kubectl create namespace kafka
curl -L https://github.com/strimzi/strimzi-kafka-operator/releases/download/0.14.0/strimzi-cluster-operator-0.14.0.yaml \
  | sed 's/namespace: .*/namespace: kafka/' \
  | kubectl apply -f - -n kafka
kubectl apply -f kubernetes/resources.yaml
kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n kafka
 
