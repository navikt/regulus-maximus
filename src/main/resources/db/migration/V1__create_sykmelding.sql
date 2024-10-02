CREATE TABLE sykmelding_behandlingsutfall
(
    sykmelding_id VARCHAR PRIMARY KEY,
    pasient_ident VARCHAR NOT NULL,
    fom           DATE NOT NULL,
    tom           DATE NOT NULL,
    sykmelding    JSONB   NOT NULL,
    metadata      JSONB   NOT NULL,
    kilde         VARCHAR NOT NULL,
    validation    JSONB   NOT NULL
);
