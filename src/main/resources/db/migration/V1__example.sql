CREATE TABLE example
(
    id   SERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid(),
    text VARCHAR,
    some_number  INT
);
