# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html). (Patch version X.Y.0 is implied if not specified.)

## [Unreleased]
### Added
- Add performance logging for report builder
- Add debug logging statements
- Enable logging of application
- Add jacoco and coveralls to pom
- Docker configuration

### Changed
- Update to aqcu-framework version 0.0.6-SNAPSHOT
- Merged this repository with the docker-aqcu-svp-report repository

## [0.0.1] - 2019-03-01
### Added
- Initial service creation
- Adding more tests
- Add qualifier start and end dates
- Add specific timeout values

### Changed
- Refactoring API calls
- Fix multiple qualifier bug
- Change how readings are retrieved
- Enabled full authentication
- Disabled TLS1.0/1.1 by default
- Fixed bug where an error would occur if there were no points between a visit and previous visit
- Multiple aqcu-framework updates
- Update to latest Aquarius SDK version

[Unreleased]: https://github.com/USGS-CIDA/aqcu-svp-report/compare/aqcu-svp-report-0.0.1...master
[0.0.1]: https://github.com/USGS-CIDA/aqcu-svp-report/tree/aqcu-svp-report-0.0.1