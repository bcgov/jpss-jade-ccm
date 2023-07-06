package ccm;

import java.util.Collections;
import java.util.Map;

import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaContainer;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {
	
	StrimziKafkaContainer kafka = new StrimziKafkaContainer();
	
	@Override
	public Map<String, String> start() {
		  
		  Log.info("starting Kafka");
		  kafka.start();
		  return Collections.singletonMap("kafka.bootstrap.servers", kafka.getBootstrapServers());  
	}
	

	@Override
	public void stop() {
		Log.info("stopping Kafka");
		kafka.close();
		kafka.stop();
		
	}

}
