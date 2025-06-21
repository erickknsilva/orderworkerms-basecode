package tech.buildrun.orderworkerms.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import tech.buildrun.orderworkerms.dto.OrderDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingProducerTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ShippingProducer shippingProducer;

    @Nested
    class publishToShippingQueue {

        @Test
        void shouldVerifyObjectMapperReturnValueOfStringAndSendMessage() throws JsonProcessingException {
            //arrange
            String orderNumber = "456";
            String email = "teste@gmail.com";
            OrderDto orderDto = spy(new OrderDto(orderNumber, email));

            ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            String expectedJson = String.format("""
                    {
                        "orderNumber": "%s",
                        "customerEmail": "%s"
                    }
                    """, orderNumber, email);

            doReturn(expectedJson).when(objectMapper).writeValueAsString(orderDto);
            //act
            shippingProducer.publishToShippingQueue(orderDto);

            //assert
            verify(sqsTemplate, times(1)).send(queueCaptor.capture(), messageCaptor.capture());

            var messageCaptured = messageCaptor.getValue();

            assertEquals("shipping-queue", queueCaptor.getValue());
            assertEquals(expectedJson, messageCaptor.getValue());
            assertTrue(messageCaptured.contains(orderNumber));
            assertTrue(messageCaptured.contains(email));
        }

    }


}