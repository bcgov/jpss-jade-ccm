[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE) [![Lifecycle:Maturing](https://img.shields.io/badge/Lifecycle-Maturing-007EC6)](<Redirect-URL>)

# JPSS Agile-integrated Digital Ecosystem - Court Case Management (JADE-CCM) Integration Solution

BC Prosecution Services (BCPS) is implementing a Digital Evidence & Disclosure Management Solution (DEMS) which will house police evidence and disclosure, electronically. BCPS is working in partnership with the Cascadia and EDT company who are providing the overall evidence management application.

The ISB Integration Delivery Services Team is responsible for providing the integration solution between EDT DEMS application and Ministry applications needed in the overall solution called JUSTIN and CORNET.

For the ISB Integration Delivery Services Team to successfully deliver a working solution (JADE-CCM), they will need to work in partnership with the DEMS project other service providers, the EDT Team responsible for the DEMS product and NTT Data responsible for JUSTIN and CORNET application changes.

To learn more about JADE and the Justice and Public Sector Integration Delivery Service, please visit: https://integrations.justice.gov.bc.ca/


## Directory Structure

```txt
src/                       - Application Root
├── main/                  - Apache Camel for Camel K integration application
│   ├── java/              - Camel K integration components and property files
│   │   └── ccm/           - Court Case Management java package
│   │       └── models/    - Court Case Management data models package
├── tests/                 - Automated test scripts and resources
├── docs/                  - Application Dcoumentation
COMPLIANCE.yaml            - BCGov PIA/STRA compliance status
LICENSE                    - License
```

## Documentation

* [Application Readme](docs/app-README.md)
* [Justice and Public Safety Sector Integrations Portal](https://integrations.justice.gov.bc.ca/)
* Product Roadmap (TBD)
* Product Wiki (TBD)
* [Openshift Readme](openshift/README.md)
* [OCIO Private OpenShift Platform Architecture Diagram (first login with IDIR via button on page, and then re-access the link)](https://cloud.gov.bc.ca/private-cloud/platform-architecture-diagram/)

## Running Tests Locally
* Run mvn compile quarkus:test in the  /jpss-jade-ccm/ directory

## License

```txt
Copyright 2023 Province of British Columbia

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
