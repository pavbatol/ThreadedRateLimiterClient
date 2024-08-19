package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Оставил этот класс, чтобы из него запускать тестирование класса CrptApi
 */

@Slf4j
public class Main {
    public static void main(String[] args) {
        log.debug("App {} started", Main.class.getName());

        //Testing CrptApi class
        System.out.printf("\nТест класса %s\n", CrptApi.class.getSimpleName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime dateTime = LocalDateTime.of(2020, 1, 23, 0, 0);
        String formattedDate = dateTime.format(formatter);

        String signature = "YOUR_SIGNATURE";
        CrptApi.Description description = new CrptApi.Description("string");

        CrptApi.Product product = CrptApi.Product.builder()
                .certificateDocument("string")
                .certificateDocumentDate(formattedDate)
                .certificateDocumentNumber("string")
                .ownerInn("string")
                .producerInn("string")
                .productionDate(formattedDate)
                .tnvedCode("string")
                .uitCode("string")
                .uituCode("string")
                .build();

        CrptApi.Document document = CrptApi.Document.builder()
                .description(description)
                .docId("string")
                .docStatus("string")
                .docType("string")
                .importRequest(true)
                .ownerInn("string")
                .participantInn("string")
                .producerInn("string")
                .productionDate("string")
                .productionType("string")
                .products(List.of(product))
                .regDate(formattedDate)
                .regNumber("string")
                .build();

        System.out.println("Проверка сериализации/десериализации экземпляра класса " + CrptApi.Document.class.getSimpleName());

        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            jsonStr = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
            System.out.println("\n— Сериализованный обьект:\n" + jsonStr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            CrptApi.Document data = objectMapper.readValue(jsonStr, CrptApi.Document.class);
            String formatedStr = ToStringBuilder.reflectionToString(data, ToStringStyle.MULTI_LINE_STYLE);
            System.out.println("\n— Десериализованный обьект:\n" + formatedStr + "\n");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        int requestLimit = 2;
        int requestAbuse = 4;
        int requestIntervalSec = 1;
        int threadsNumber = requestLimit + requestAbuse;
        CrptApi client = new CrptApi(requestIntervalSec, requestLimit);

        System.out.println("Кол-во потоков: " + threadsNumber);
        System.out.println("Лимит запросов: " + requestLimit);
        System.out.println("Интервал (сек): " + requestIntervalSec + "\n");

        Thread[] threads = new Thread[threadsNumber];
        for (int i = 0; i < threadsNumber; i++) {
            threads[i] = new Thread(() -> {
                client.post(document, signature);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        log.debug("All {} threads have completed execution", threadsNumber);
        System.out.println("\nDone");
    }
}
