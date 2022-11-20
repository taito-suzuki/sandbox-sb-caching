import random

for _ in range(1000000):
    i = random.randint(1, 2000)
    print(f"GET http://localhost:8080/comments/{i}")