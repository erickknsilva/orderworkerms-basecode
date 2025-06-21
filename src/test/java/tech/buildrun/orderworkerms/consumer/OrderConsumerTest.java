package tech.buildrun.orderworkerms.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.buildrun.orderworkerms.dto.OrderEventDto;
import tech.buildrun.orderworkerms.service.OrderProcessingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock
    private OrderProcessingService orderProcessingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderConsumer orderConsumer;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;

    @Nested
    class consume {

        @Test
        void shouldProcessOrderWhenIsOkay() throws JsonProcessingException {
            //arrange
            String messageOrderNumber = "456";
            OrderEventDto orderEventDto = new OrderEventDto(messageOrderNumber);
            doReturn(orderEventDto).when(objectMapper).readValue(messageOrderNumber, OrderEventDto.class);

            //act
            orderConsumer.consume(messageOrderNumber);

            //assert
            verify(orderProcessingService, times(1)).processOrder(argumentCaptor.capture());

            var argumentCaptured = argumentCaptor.getValue();
            assertEquals(messageOrderNumber, argumentCaptured);

        }

        @Test
        void shouldThrowRuntimeException() throws JsonProcessingException {
            //arrange
            String messageOrderNumber = "456";

           var result = assertThrows(RuntimeException.class, ()->{
                orderConsumer.consume(messageOrderNumber);
            });

           assertEquals("Failed to process message", result.getMessage());
           verifyNoInteractions(orderProcessingService);

        }

    }

}