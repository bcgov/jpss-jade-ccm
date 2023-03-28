# How-to setup local runtime

This document provides instructions around how to run JADE-CCM locally.

## Deploloy JADE-Events (Kafka) locally

1. Install Docker Desktop locally
2. Run 'redeploy-events' script from repo base directory
    - You should see docker compose (jade-events) started with the following two containers:
        - zookeepr
        - broker

## Deploy JADE-CCM locally

1. Follow the instructions here https://camel.apache.org/manual/camel-jbang.html to install JBang and Camel JBang.

2. After making your changes to the Jade CCM classes you will have to run the jpss-jade-ccm/build-install-ccm-models script which will 
    a) rebuild ccm-models.jar file
    b) install in the local Maven repository

3. Run the script to run the Mock services for Justin, DEMS and Splunk : 
    a) ./redeploy-mock-dems-app local
    b) ./redeploy-mock-justin-app local
    c) ./redeploy-mock-splunk-app local

4. Run the redeploy script for the program you which to have running locally. For example, for DEMS Adapter run : 
./redeploy-ccm-dems-adapter local
