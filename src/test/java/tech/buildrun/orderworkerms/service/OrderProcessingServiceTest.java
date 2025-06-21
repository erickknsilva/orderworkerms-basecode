package tech.buildrun.orderworkerms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.buildrun.orderworkerms.dto.OrderDto;
import tech.buildrun.orderworkerms.entity.Order;
import tech.buildrun.orderworkerms.producer.ShippingProducer;
import tech.buildrun.orderworkerms.repository.OrderRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShippingProducer shippingProducer;

    @InjectMocks
    private OrderProcessingService processingService;


    @Captor
    private ArgumentCaptor<OrderDto> argumentCaptorOrderDto;

    @Nested
    class processOrder {

        @Test
        void shouldReturnOrderWhenExist() throws JsonProcessingException {
            //arrange
            long id = 123L;
            String orderNumber = "456";
            String customerEmail = "teste@gmail.com";
            boolean notified = false;

            Order order = spy(new Order("456", "teste@gmail.com", false));
            doReturn(Optional.of(order)).when(orderRepository).findByOrderNumber(orderNumber);
            //act
            processingService.processOrder(orderNumber);

            //assert
            verify(orderRepository).findByOrderNumber(orderNumber);
            assertTrue(order.isNotified());
        }

        @Test
        void shouldReturnExceptionWhenOrderDoesNotExist() {
            //arrange
            String orderNumber = "456";
            doReturn(Optional.empty()).when(orderRepository)
                    .findByOrderNumber(orderNumber);

            //act
            var result = assertThrows(RuntimeException.class, () -> {
                processingService.processOrder(orderNumber);
            });

            //assert
            assertEquals("Order not found: " + orderNumber, result.getMessage());
            verify(orderRepository, never()).save(any());
            verifyNoInteractions(shippingProducer);
        }

        @Test
        void shouldSendCorrectDtoToShippingProducer() throws JsonProcessingException {
            //arrange
            long id = 123L;
            String orderNumber = "456";
            String customerEmail = "teste@gmail.com";
            boolean notified = false;

            Order order = spy(new Order(orderNumber, customerEmail, notified));
            OrderDto orderDto = new OrderDto(order.getOrderNumber(), order.getCustomerEmail());

            doReturn(Optional.of(order))
                    .when(orderRepository).findByOrderNumber(orderNumber);

            //act
            processingService.processOrder(orderNumber);
            verify(shippingProducer).publishToShippingQueue(argumentCaptorOrderDto.capture());
            var capture = argumentCaptorOrderDto.getValue();

            assertEquals(order.getOrderNumber(), capture.orderNumber());
            assertEquals(order.getCustomerEmail(), capture.customerEmail());

        }

        @Test
        void shouldSaveOrderAfterNotifying() throws JsonProcessingException {
            //arrange
            long id = 123L;
            String orderNumber = "456";
            String customerEmail = "teste@gmail.com";
            boolean notified = false;

            Order order = spy(new Order(orderNumber, customerEmail, notified));
            doReturn(Optional.of(order)).when(orderRepository).findByOrderNumber(orderNumber);

            //act e Assert
            processingService.processOrder(orderNumber);
            verify(orderRepository).save(order);
            assertTrue(order.isNotified());

            assertEquals(orderNumber, order.getOrderNumber());
            assertEquals(customerEmail, order.getCustomerEmail());

        }

        @Test
        void shouldReturnFalseWhenOrderIsNotNotified() {
            //arrange
            String orderNumber = "456";
            String customerEmail = "teste@gmail.com";
            boolean notified = false;

            Order order = new Order(orderNumber, customerEmail, notified);

            //act e assert
            assertFalse(order.isNotified());
        }

        @Test
        void shouldReturnTrueWhenOrderIsNotified() {
            //arrange
            String orderNumber = "456";
            String customerEmail = "teste@gmail.com";
            boolean notified = true;

            Order order = new Order(orderNumber, customerEmail, notified);

            //act e assert
            assertTrue(order.isNotified());
        }
    }

}