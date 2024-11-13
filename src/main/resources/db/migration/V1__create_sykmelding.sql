CREATE TABLE sykmelding
(
    sykmelding_id VARCHAR PRIMARY KEY,
    pasient_ident VARCHAR NOT NULL,
    fom           DATE NOT NULL,
    tom           DATE NOT NULL,
    sykmelding    JSONB   NOT NULL,
    validation    JSONB   NOT NULL,
    generated_date TIMESTAMP WITH TIME ZONE,
    metadata jsonb NOT NULL
);
