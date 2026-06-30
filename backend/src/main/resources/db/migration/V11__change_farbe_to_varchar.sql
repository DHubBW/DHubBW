ALTER TABLE ordner
    ALTER COLUMN farbe TYPE VARCHAR(50)
        USING CASE farbe
                  WHEN 0 THEN 'DEFAULT'
                  WHEN 1 THEN 'RED'
                  WHEN 2 THEN 'BLUE'
                  WHEN 3 THEN 'GREEN'
    END;
