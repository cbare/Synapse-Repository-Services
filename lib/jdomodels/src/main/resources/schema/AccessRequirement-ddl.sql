CREATE TABLE `ACCESS_REQUIREMENT` (
  `ID` bigint(20) NOT NULL,
  `ETAG` char(36) NOT NULL,
  `CREATED_BY` bigint(20) NOT NULL,
  `CREATED_ON` bigint(20) NOT NULL,
  `MODIFIED_BY` bigint(20) NOT NULL,
  `MODIFIED_ON` bigint(20) NOT NULL,
  `ACCESS_TYPE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `ENTITY_TYPE` varchar(256) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `SERIALIZED_ENTITY` mediumblob,
  PRIMARY KEY (`ID`),
  CONSTRAINT `ACCESS_REQUIREMENT_CREATED_BY_FK` FOREIGN KEY (`CREATED_BY`) REFERENCES `JDOUSERGROUP` (`ID`),
  CONSTRAINT `ACCESS_REQUIREMENT_MODIFIED_BY_FK` FOREIGN KEY (`MODIFIED_BY`) REFERENCES `JDOUSERGROUP` (`ID`)
)