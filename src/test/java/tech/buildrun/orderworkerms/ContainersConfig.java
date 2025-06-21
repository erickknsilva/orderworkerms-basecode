package tech.buildrun.orderworkerms;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import java.net.URI;
import java.util.Map;

import static tech.buildrun.orderworkerms.consumer.OrderConsumer.ORDER_CONFIRMED_QUEUE;
import static tech.buildrun.orderworkerms.producer.ShippingProducer.SHIPPING_QUEUE;

@Testcontainers
public class ContainersConfig {

    @Container
    static LocalStackContainer localStackContainer = new LocalStackContainer
            (DockerImageName.parse("localstack/localstack:4.5.0"));

    //sobrescrevendo as configurações do spring aws para apontar para o localStack
    public static Map<String, String> getProperties() {

        return Map.of
                ("spring.cloud.aws.endpoint", getLocalStackUrl(),
                        "spring.cloud.aws.credentials.access-key", "test",
                        "spring.cloud.aws.credentials.secret-key", "test",
                        "spring.cloud.aws.region.static", "sa-east-1"
                );
    }

    @DynamicPropertySource
    public static void dinamicPropertySource(DynamicPropertyRegistry registry) {
        getProperties().entrySet().forEach(kv -> {
            registry.add(kv.getKey(), kv::getValue);
        });
    }

    private static String getLocalStackUrl() {
        return "http://" + localStackContainer.getHost() + ":"
                + localStackContainer.getMappedPort(4566);
    }

    public static SqsClient getSqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create(getLocalStackUrl()))
                .credentialsProvider(StaticCredentialsProvider
                        .create(AwsBasicCredentials.create("test", "test")))
                .region(Region.SA_EAST_1)
                .build();
    }

    public static void setupSqs() {

        var sqsCient = getSqsClient();
        sqsCient.createQueue(CreateQueueRequest.builder()
                .queueName(ORDER_CONFIRMED_QUEUE)
                .build());

        sqsCient.createQueue(CreateQueueRequest.builder()
                .queueName(SHIPPING_QUEUE)
                .build());
    }
}
