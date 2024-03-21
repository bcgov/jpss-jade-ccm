# How-to setup local runtime

This document provides instructions around how to run ISL-CCM locally.

## Deploloy ISL-Events (Kafka) locally

1. Install Docker Desktop locally
2. Run 'redeploy-events' script from repo base directory
    - You should see docker compose (isl-events) started with the following two containers:
        - zookeepr
        - broker

## Deploy ISL-CCM locally
Before attempting to run locally, the ISL-Events instructions above will have to be carried out for a local Kafka server to be established. 

1. Follow the instructions here https://camel.apache.org/manual/camel-jbang.html to install JBang and Camel JBang.

2. After making your changes to the isl CCM classes, you will have to run the jpss-isl-ccm/build-install-ccm-models script which will:
    a) rebuild ccm-models.jar file
    b) install in the local Maven repository

3. Run the script to run the Mock services for Justin, DEMS and Splunk : 
    a) ./redeploy-mock-dems-app local
    b) ./redeploy-mock-justin-app local
    c) ./redeploy-mock-splunk-app local

4. Run the redeploy script for the program you which to have running locally. For example, these could be run: 
./redeploy-ccm-dems-adapter local
./redeploy-ccm-lookup-service local
./redeploy-ccm-notification-service local
./redeploy-ccm-splunk-adapter local
./redeploy-ccm-justin-adapter local
./redeploy-ccm-pidp-adapter local (this will require more set up for pidp specific end points, likely to need a mock PIDPAdapter )


Please note these will ALL have to be run in a separate console window and using Bash. 

