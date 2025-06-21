package tech.buildrun.orderworkerms;

import org.springframework.boot.SpringApplication;

import java.util.List;

import static tech.buildrun.orderworkerms.ContainersConfig.*;

public class OrderworkermsApplicationTest {


    public static void main(String[] args) {

        localStackContainer.setPortBindings(List.of("4566:4566"));
        localStackContainer.start();
        setupSqs();
        getProperties().forEach(System::setProperty);

        SpringApplication.from(OrderworkermsApplication::main)
                .with(ServiceConnectionConfig.class)
                .run(args);
    }
}