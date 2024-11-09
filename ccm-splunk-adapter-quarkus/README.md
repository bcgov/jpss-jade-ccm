
# CCM Acess Deduplication Processor

A Kafka Streams processor designed to deduplicate incoming messages to ensure that each unique message is processed only once.

## Table of Contents
6. [Local Docker Deployment](#local-deploy)

---

### 1. Key Features
- Utilizes Kafka's `AbstractProcessor` to process and filter out duplicate messages.
- Uses a Kafka `KeyValueStore` to track previously processed messages.

### 2. Prerequisites
- Java 11
- Apache Maven

### 3. Dependencies
- Quarkus version: 2.3.1.Final
- Apache Kafka version: 2.8.1
- JUnit version: 4.13.2

### 4. Setup and Usage

1. Clone the repository:
   ```
   bash
   git clone <repository_url>
   ```

2. Navigate to the project directory:
   ```bash
   cd ccm-accessdedup-processor
   ```

3. Build the project using Maven:
   ```
   bash
   mvn clean install
   ```

4. Run the application (add any necessary commands specific to your application).

### 5. Testing

The project includes unit tests for the deduplication processor using Kafka's `TopologyTestDriver`. 

To run the tests:
```bash
mvn test
```

### 6. Local Docker Deployment <a name="local-deploy"></a>

1. Make sure JADE-Events are already running
   ```
   cd scripts/jade-events
   docker-compose up
   ```

2. Navigate to the scripts directory
   ```
   cd scripts/jade-ccm
   ```

3. Execute the build and deploy script, specifying local deployment
   ```
   bash redeploy-ccm-accessdedeup-processor local
   ```


### 7. Configuration

Ensure that you have Kafka up and running, and adjust any Kafka configurations as necessary in the application's properties or configuration files.

### 8. Running Locally with Quarkus

If you're using Quarkus, you can run the `ccm-accessdedup-processor` in development mode. This mode provides live coding capabilities, allowing for real-time updates without restarting the application.

1. **Ensure Kafka is Running**: Make sure you have a local instance of Kafka running.
2. **Navigate to the Project Directory**: This should be the directory containing your `pom.xml` file.
3. **Run Quarkus in Development Mode**:
   ```bash
   mvn quarkus:dev
   ```
   This will start your Quarkus application with the `ccm-accessdedup-processor` in development mode.
4. **Monitor**: Your processor should now be running and processing messages from Kafka topics. Produce messages to the input topic and monitor the deduplication results.
5. **Live Coding**: With `mvn quarkus:dev`, you can make changes to your code and see the results immediately. Quarkus will automatically recompile and apply those changes in real-time.
6. **Shutdown**: To stop the processor and exit development mode, press `Ctrl + C` in your terminal.

Ensure your application's configuration (e.g., `application.properties`) points to your local Kafka instance, typically `localhost:9092` for a local setup.
