article_id = 0
for user_id in range(100):
    print(f"INSERT INTO users (id, name) VALUES ({user_id}, 'user {user_id}');")
    for _ in range(100):
        print(f"INSERT INTO articles (id, title, author_id) VALUES ({article_id}, 'article {article_id}', {user_id});")
        article_id += 1