DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'regulus-maximus-instance')
        THEN
            ALTER USER "regulus-maximus-instance" IN DATABASE "regulus-maximus" SET pgaudit.log TO 'none';
        END IF;
    END
$$;
