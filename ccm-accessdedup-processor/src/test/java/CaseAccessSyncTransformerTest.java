import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.quarkus.test.junit.QuarkusTest;

import javax.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@QuarkusTest
public class CaseAccessSyncTransformerTest {

    @Mock
    private ProcessorContext context;
    @Mock
    private KeyValueStore<String, String> accessdedupStore;
    @Mock
    private KeyValueIterator<String, String> iterator;

    private CaseAccessSyncTransformer transformer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(context.getStateStore(anyString())).thenReturn(accessdedupStore);
        transformer = new CaseAccessSyncTransformer();
        transformer.init(context);
    }

    //@Test
    public void testTransformEndOfBatch() {
        when(accessdedupStore.all()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true, false); // simulate one entry in the store
        when(iterator.next()).thenReturn(new KeyValue<>("existing_key", "existing_value"));

        // Act
        transformer.transform("END_OF_BATCH", "irrelevant_value");

        // Assert
        verify(context, times(1)).forward(eq("existing_key"), eq("existing_value")); // Forwarded once
        verify(accessdedupStore, times(1)).delete(eq("existing_key")); // Deleted once
    }

    //@Test
    public void testTransformWithExistingKey() {
        String existingKey = "existing_key";
        String existingValue = "existing_value";
        when(accessdedupStore.get(existingKey)).thenReturn(existingValue);

        // Act
        transformer.transform(existingKey, "new_value");

        // Assert
        verify(accessdedupStore, never()).put(anyString(), anyString()); // The store should not be updated
        verify(context, never()).forward(anyString(), anyString()); // No forwarding should occur
    }

    // Other tests can be added here...
    @Test
    public void testTransformWithNewKey() {
        String newKey = "new_key";
        String newValue = "new_value";
        
        // Act
        transformer.transform(newKey, newValue);

        // Assert
        verify(accessdedupStore, times(1)).put(eq(newKey), eq(newValue)); // The store should be updated
    }
}
