import random

for _ in range(100):
    i = random.randint(1, 90)
    print(f"GET http://localhost:8080/users/{i}")