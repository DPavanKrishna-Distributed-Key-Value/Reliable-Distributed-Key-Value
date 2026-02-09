import random
import datetime
import json

# Generate 500 user session data entries
sessions = []
user_ids = [f"user{i:03d}" for i in range(1, 501)]

for user_id in user_ids:
    login_time = datetime.datetime.now() - datetime.timedelta(minutes=random.randint(0, 1440))  # Random login time in last 24 hours
    status = random.choice(["active", "inactive", "expired"])
    session_data = {
        "userId": user_id,
        "loginTime": login_time.strftime("%H:%M"),
        "status": status
    }
    sessions.append(f"session:{user_id} → {json.dumps(session_data)}")

# Save to file
with open('user_sessions.txt', 'w', encoding='utf-8') as f:
    f.write("\n".join(sessions))

print("Generated 500 sessions and saved to user_sessions.txt")