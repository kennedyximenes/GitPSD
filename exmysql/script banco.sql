CREATE TABLE IF NOT EXISTS ponto (
   id    INT UNSIGNED  NOT NULL AUTO_INCREMENT,
   latitude  VARCHAR(20),
   longitude VARCHAR(20),
   texto     VARCHAR(50),
   PRIMARY KEY  (id)
);