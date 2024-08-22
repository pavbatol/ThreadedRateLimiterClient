package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Оставил этот класс, чтобы из него запускать тестирование класса CrptApi
 */

@Slf4j
public class Main {
    private static final String TEXT_DEFAULT_PARAMS = "параметры по умолчанию";
    private static final String TEXT_SPECIFIED_PARAMS = "указать свои параметры";

    public static void main(String[] args) {
        log.debug("App {} started", Main.class.getName());

        //Testing CrptApi class
        Scanner scanner = new Scanner(System.in);
        MenuResult menuResult = getMenuResult(scanner, 1, 2, 6);

        if (menuResult.choice == 0) {
            System.out.println("\nВыход из программы.");
            System.exit(0);
        }

        int requestIntervalSec = menuResult.requestIntervalSec;
        int requestLimit = menuResult.requestLimit;
        int threadsNumber = menuResult.threadsNumber;

        System.out.printf("\nТест класса %s (%s)\n",
                CrptApi.class.getSimpleName(), (menuResult.choice == 1 ? TEXT_DEFAULT_PARAMS : TEXT_SPECIFIED_PARAMS));

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

    private static MenuResult getMenuResult(Scanner scanner, int requestIntervalSec, int requestLimit, int threadsNumber) {
        int choice;
        while (true) {
            System.out.println("\nВыберите действие:");
            System.out.println("1. Тест класса CrptApi (" + TEXT_DEFAULT_PARAMS + ")");
            System.out.printf("\t- %-18s: %d\n", "количество потоков", threadsNumber);
            System.out.printf("\t- %-18s: %d\n", "лимит запросов", requestLimit);
            System.out.printf("\t- %-18s: %d\n", "интервал (сек)", requestIntervalSec);
            System.out.println("2. Тест класса CrptApi (" + TEXT_SPECIFIED_PARAMS + ")");
            System.out.println("0. Выход");
            System.out.print("Ваш выбор (введите номер): ");

            if (scanner.hasNextInt()) {
                choice = scanner.nextInt();
                if (choice < 0 || choice > 2) {
                    System.out.println("Некорректный выбор. Пожалуйста, введите номер от 0 до 2.");
                } else {
                    break;
                }
            } else {
                System.out.println("Некорректный ввод. Пожалуйста, введите целое число.");
                scanner.next();
            }
        }

        if (choice != 1) {
            while (choice != 0) {
                System.out.println("\n0. Выход");
                System.out.print("Укажите интервал в секундах: ");

                if (scanner.hasNextInt()) {
                    requestIntervalSec = scanner.nextInt();
                    if (requestIntervalSec == 0) {
                        choice = 0;
                    }
                    if (requestIntervalSec < 0 || requestIntervalSec > 3600) {
                        System.out.println("Некорректный выбор. Пожалуйста, введите номер от 0 до 3600.");
                    } else {
                        break;
                    }
                } else {
                    System.out.println("Некорректный ввод. Пожалуйста, введите целое число.");
                    scanner.next();
                }
            }

            while (choice != 0) {
                System.out.println("\n0. Выход");
                System.out.print("Укажите лимит запросов за интервал: ");

                if (scanner.hasNextInt()) {
                    requestLimit = scanner.nextInt();
                    if (requestLimit == 0) {
                        choice = 0;
                    }
                    if (requestLimit < 0 || requestLimit > 1000) {
                        System.out.println("Некорректный выбор. Пожалуйста, введите номер от 0 до 1000.");
                    } else {
                        break;
                    }
                } else {
                    System.out.println("Некорректный ввод. Пожалуйста, введите целое число.");
                    scanner.next();
                }
            }

            while (choice != 0) {
                System.out.println("\n0. Выход");
                System.out.print("Укажите количество потоков: ");

                if (scanner.hasNextInt()) {
                    threadsNumber = scanner.nextInt();
                    if (threadsNumber == 0) {
                        choice = 0;
                    }
                    if (threadsNumber < 0 || threadsNumber > 10) {
                        System.out.println("Некорректный выбор. Пожалуйста, введите номер от 0 до 10.");
                    } else {
                        break;
                    }
                } else {
                    System.out.println("Некорректный ввод. Пожалуйста, введите целое число.");
                    scanner.next();
                }
            }
        }

        return new MenuResult(choice, requestIntervalSec, requestLimit, threadsNumber);
    }

    private record MenuResult(int choice, int requestIntervalSec, int requestLimit, int threadsNumber) {
    }
}
