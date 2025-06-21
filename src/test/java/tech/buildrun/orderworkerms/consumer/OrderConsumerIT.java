package tech.buildrun.orderworkerms.consumer;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import tech.buildrun.orderworkerms.ContainersConfig;
import tech.buildrun.orderworkerms.ServiceConnectionConfig;
import tech.buildrun.orderworkerms.dto.OrderDto;
import tech.buildrun.orderworkerms.dto.OrderEventDto;
import tech.buildrun.orderworkerms.entity.Order;
import tech.buildrun.orderworkerms.repository.OrderRepository;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static tech.buildrun.orderworkerms.consumer.OrderConsumer.ORDER_CONFIRMED_QUEUE;
import static tech.buildrun.orderworkerms.producer.ShippingProducer.SHIPPING_QUEUE;

@Import(ServiceConnectionConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderConsumerIT extends ContainersConfig {


    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    public static void beforeAll() {
        setupSqs();
    }

    @BeforeEach
    void beforeEach() {
        orderRepository.deleteAll();
        SqsClient sqsClient = getSqsClient();
        sqsClient.purgeQueue(PurgeQueueRequest.builder()
                .queueUrl(ORDER_CONFIRMED_QUEUE).build());

        sqsClient.purgeQueue(PurgeQueueRequest.builder()
                .queueUrl(SHIPPING_QUEUE).build());
    }

    @Test
    void whenExistingOrderShouldPublishToShippingQueue() throws JsonProcessingException {
        //arrange
        String orderNumber = "1345";
        Order order = new Order(orderNumber, "teste@gmail.com", false);
        orderRepository.save(order);

        var event = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(event);

        //act
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE, payload);

        //assert
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var message = sqsTemplate.receive(SHIPPING_QUEUE, String.class);
            assertTrue(message.isPresent());

            OrderDto orderDto = objectMapper.readValue(message.get().getPayload(), OrderDto.class);
            assertEquals(orderNumber, orderDto.orderNumber());
            assertEquals(order.getCustomerEmail(), orderDto.customerEmail());
        });

    }

    @Test
    void whenExistingOrderShouldUpdateDatabase() throws JsonProcessingException {

        //arrange
        String orderNumber = "1345";
        Order order = new Order(orderNumber, "teste@gmail.com", false);
        orderRepository.save(order);

        var event = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(event);

        //act
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE, payload);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var orderDb = orderRepository.findByOrderNumber(orderNumber);
            assertTrue(orderDb.isPresent());
            assertTrue(orderDb.get().isNotified());
        });
    }

    @Test
    void whenOrderNotFoundShouldNotPublishToShippingQueue() throws JsonProcessingException {
        //arrange
        String orderNumber = "1345";
        var event = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(event);

        //act
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE, payload);

        //assert
        await().atMost(12, TimeUnit.SECONDS).untilAsserted(()->{
            var message = sqsTemplate.receive(SHIPPING_QUEUE, String.class);
            assertTrue(message.isEmpty());
        });
    }


}
