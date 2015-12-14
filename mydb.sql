CREATE SCHEMA IF NOT EXISTS `mydb` DEFAULT CHARACTER SET latin1 ;
CREATE TABLE IF NOT EXISTS `mydb`.`access_lint` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `Domain` VARCHAR(45) NULL DEFAULT NULL,
  `Url` VARCHAR(200) NULL DEFAULT NULL,
  `Time` DATETIME NULL DEFAULT NULL,
  `ARIA-1` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-2` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-3` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-4` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-5` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-6` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-7` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-8` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-9` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-10` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-11` VARCHAR(6) NULL DEFAULT 'NA',
  `ARIA-12` VARCHAR(6) NULL DEFAULT 'NA',
  `AUDIO-1` VARCHAR(6) NULL DEFAULT 'NA',
  `COLOR-1` VARCHAR(6) NULL DEFAULT 'NA',
  `FOCUS-1` VARCHAR(6) NULL DEFAULT 'NA',
  `FOCUS-2` VARCHAR(6) NULL DEFAULT 'NA',
  `FOCUS-3` VARCHAR(6) NULL DEFAULT 'NA',
  `HTML-1` VARCHAR(6) NULL DEFAULT 'NA',
  `HTML-2` VARCHAR(6) NULL DEFAULT 'NA',
  `IMAGE-1` VARCHAR(6) NULL DEFAULT 'NA',
  `TEXT-1` VARCHAR(6) NULL DEFAULT 'NA',
  `TEXT-2` VARCHAR(6) NULL DEFAULT 'NA',
  `TEXT-4` VARCHAR(6) NULL DEFAULT 'NA',
  `TITLE-1` VARCHAR(6) NULL DEFAULT 'NA',
  `VIDEO-1` VARCHAR(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB
AUTO_INCREMENT = 2116
DEFAULT CHARACTER SET = latin1