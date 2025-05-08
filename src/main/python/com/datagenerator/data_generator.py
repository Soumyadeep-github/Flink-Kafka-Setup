from faker import Faker
import json
import random
import time
from datetime import datetime, timedelta
from kafka import KafkaProducer
import uuid

KAFKA_BROKER = "localhost:9093"

'''
if __name__ == "__main__":
    broker = [
        "localhost:9093"
    ]
    producer = KafkaProducer(
        bootstrap_servers=broker,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )

    faker = Faker()

    while True:
        data = {
            "user": {
                "id": random.randint(1, 100),
                "name": faker.name(),
                "location": faker.city(),
            },
            "event": {
                "type": random.choice(["click", "purchase", "view"]),
                "timestamp": faker.unix_time(),
                "amount": round(random.uniform(10, 500), 2)
            }
        }
        print(data)
        producer.send("test-topic", value=data)
        print(f"Sent: {data}")
        time.sleep(3)

'''
def generate_user_activity():
    """Generate complex user activity data"""
    user_id = str(uuid.uuid4())
    timestamp = datetime.now().isoformat()
    
    # Generate nested location data
    location = {
        "country": fake.country(),
        "city": fake.city(),
        "coordinates": {
            "latitude": float(fake.latitude()),
            "longitude": float(fake.longitude())
        },
        "timezone": fake.timezone()
    }
    
    # Generate device info
    device = {
        "type": random.choice(["mobile", "desktop", "tablet"]),
        "os": random.choice(["iOS", "Android", "Windows", "MacOS"]),
        "browser": random.choice(["Chrome", "Safari", "Firefox", "Edge"]),
        "version": f"{random.randint(1, 100)}.{random.randint(0, 9)}.{random.randint(0, 9)}"
    }
    
    # Generate feature usage data
    features = {
        "feature_id": str(uuid.uuid4()),
        "name": random.choice(["search", "profile", "payment", "notification", "settings"]),
        "action": random.choice(["view", "click", "hover", "scroll"]),
        "duration": random.randint(1, 300),  # seconds
        "success": random.choice([True, False])
    }
    
    # Generate session data
    session = {
        "id": str(uuid.uuid4()),
        "start_time": (datetime.now() - timedelta(minutes=random.randint(1, 60))).isoformat(),
        "page_views": random.randint(1, 20),
        "interaction_count": random.randint(1, 50)
    }
    
    return {
        "event_id": str(uuid.uuid4()),
        "user_id": user_id,
        "timestamp": timestamp,
        "event_type": "user_activity",
        "location": location,
        "device": device,
        "features": features,
        "session": session,
        "metadata": {
            "version": "1.0",
            "source": "web_application",
            "environment": random.choice(["production", "staging", "development"])
        }
    }

def generate_transaction():
    """Generate complex transaction data"""
    transaction_id = str(uuid.uuid4())
    timestamp = datetime.now().isoformat()
    
    # Generate nested payment details
    payment = {
        "method": random.choice(["credit_card", "debit_card", "paypal", "bank_transfer"]),
        "currency": random.choice(["USD", "EUR", "GBP", "JPY"]),
        "amount": round(random.uniform(10.0, 1000.0), 2),
        "status": random.choice(["pending", "completed", "failed", "refunded"]),
        "card_details": {
            "type": random.choice(["visa", "mastercard", "amex"]),
            "last_four": str(random.randint(1000, 9999)),
            "expiry_month": random.randint(1, 12),
            "expiry_year": random.randint(2024, 2030)
        }
    }
    
    # Generate nested merchant data
    merchant = {
        "id": str(uuid.uuid4()),
        "name": fake.company(),
        "category": random.choice(["retail", "food", "travel", "entertainment"]),
        "location": {
            "country": fake.country(),
            "city": fake.city()
        }
    }
    
    # Generate nested customer data
    customer = {
        "id": str(uuid.uuid4()),
        "name": fake.name(),
        "email": fake.email(),
        "segment": random.choice(["premium", "standard", "basic"]),
        "loyalty_points": random.randint(0, 1000)
    }
    
    # Generate nested risk assessment
    risk = {
        "score": random.randint(0, 100),
        "factors": random.sample([
            "high_amount", "new_merchant", "unusual_location",
            "multiple_attempts", "velocity_check"
        ], random.randint(1, 5)),
        "recommendation": random.choice(["approve", "review", "decline"])
    }
    
    return {
        "event_id": str(uuid.uuid4()),
        "transaction_id": transaction_id,
        "timestamp": timestamp,
        "event_type": "transaction",
        "payment": payment,
        "merchant": merchant,
        "customer": customer,
        "risk": risk,
        "metadata": {
            "version": "1.0",
            "source": "payment_gateway",
            "environment": random.choice(["production", "staging", "development"])
        }
    }

# Create topics if they don't exist
# def create_topics():
#     from kafka.admin import KafkaAdminClient, NewTopic
    
#     admin_client = KafkaAdminClient(
#         bootstrap_servers=[KAFKA_BROKER]
#     )
    
#     topics = [
#         NewTopic(name="user_activities", num_partitions=3, replication_factor=3),
#         NewTopic(name="transactions", num_partitions=3, replication_factor=3)
#     ]
    
#     try:
#         admin_client.create_topics(topics)
#         print("Topics created successfully")
#     except Exception as e:
#         print(f"Error creating topics: {e}")

# Main loop to produce messages
def produce_messages(producer):

    
    while True:
        # Generate and send user activity
        user_activity = generate_user_activity()
        producer.send('user_activities', value=user_activity)
        
        # Generate and send transaction
        transaction = generate_transaction()
        producer.send('transactions', value=transaction)
        
        print(f"Sent user activity: {user_activity['event_id']}")
        print(f"Sent transaction: {transaction['transaction_id']}")
        
        # time.sleep(5)  # Adjust the rate as needed

if __name__ == "__main__":
    fake = Faker()

    # Initialize Kafka producer
    producer = KafkaProducer(
        bootstrap_servers=[KAFKA_BROKER],
        value_serializer=lambda x: json.dumps(x).encode('utf-8')
    )

    produce_messages(producer)
