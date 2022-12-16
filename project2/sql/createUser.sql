DO
$do$
    BEGIN
        IF NOT (EXISTS(SELECT * from pg_catalog.pg_user WHERE pg_user.usename = 'courier')) THEN
            CREATE USER courier PASSWORD 'courier_password';
            CREATE USER officer WITH PASSWORD 'officer_password';
            CREATE USER company_manager WITH PASSWORD 'company_manager_password';
            CREATE USER sustc_manager WITH PASSWORD 'sustc_manager_password';

            GRANT SELECT, UPDATE ON TABLE item,courier TO courier;
            GRANT SELECT, UPDATE ON TABLE item,officer TO officer;
            GRANT SELECT ON TABLE ship TO company_manager;
            GRANT SELECT, UPDATE ON TABLE item,company TO company_manager;
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO sustc_manager;
        END IF;
    END;
$do$;
