-- ----------------------------------------------------------------------------
-- MySQL Workbench Migration
-- Migrated Schemata: mydb2
-- Source Schemata: mydb
-- Created: Fri Mar  4 01:13:39 2016
-- ----------------------------------------------------------------------------

SET FOREIGN_KEY_CHECKS = 0;;

-- ----------------------------------------------------------------------------
-- Schema mydb2
-- ----------------------------------------------------------------------------
DROP SCHEMA IF EXISTS `mydb` ;
CREATE SCHEMA IF NOT EXISTS `mydb` ;

-- ----------------------------------------------------------------------------
-- Table mydb.access_lint
-- ----------------------------------------------------------------------------
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
AUTO_INCREMENT = 12
DEFAULT CHARACTER SET = latin1;

-- ----------------------------------------------------------------------------
-- Table mydb.html_codesniffer
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`html_codesniffer` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `Domain` VARCHAR(45) NULL DEFAULT NULL,
  `Url` VARCHAR(200) NULL DEFAULT NULL,
  `Time` DATETIME NULL DEFAULT NULL,
  `Errors` VARCHAR(500) NULL DEFAULT NULL,
  `Notices` VARCHAR(500) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC))
ENGINE = InnoDB
AUTO_INCREMENT = 3
DEFAULT CHARACTER SET = latin1;
SET FOREIGN_KEY_CHECKS = 1;;
