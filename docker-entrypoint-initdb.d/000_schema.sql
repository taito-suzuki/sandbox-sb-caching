CREATE TABLE IF NOT EXISTS users (
    id SERIAL NOT NULL,
    name VARCHAR(128) NOT NULL,
    PRIMARY KEY(id)
);

CREATE TABLE IF NOT EXISTS articles (
    id SERIAL NOT NULL,
    title VARCHAR(128) NOT NULL,
    author_id INT NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT fk_articles_author_id
    FOREIGN KEY(author_id)
    REFERENCES users(id)
    ON DELETE NO ACTION
);