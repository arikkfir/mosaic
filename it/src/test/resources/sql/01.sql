-- *********************************************
-- THE "mosaic" SCHEMA
-- *********************************************
DROP SCHEMA IF EXISTS "mosaic";
CREATE SCHEMA "mosaic";

-- *********************************************
-- THE "it" SCHEMA
-- *********************************************
DROP SCHEMA IF EXISTS "it";
CREATE SCHEMA "it";

CREATE TABLE "it.it01_table1" (

  "id"   INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
  "name" VARCHAR(50) NOT NULL

);