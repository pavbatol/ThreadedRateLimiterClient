package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CrptApi {
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final String AUTH_TOKEN = "Bearer ВАШ_ТОКЕН";
    private static final String SIGNATURE = "ВАШF_ПОДПИСЬ";
    private static final Object lock = new Object();
    private static long intervalStartMs = System.currentTimeMillis();
    private static int requestCount = 0;

    private final int requestLimit;
    private final long requestIntervalMs;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this(timeUnit, 1, requestLimit);
    }

    public CrptApi(int requestIntervalSec, int requestLimit) {
        this(TimeUnit.SECONDS, requestIntervalSec, requestLimit);
    }

    public CrptApi(TimeUnit timeUnit, int timeValue, int requestLimit) {
        if (timeValue <= 0 || requestLimit <= 0) {
            throw new IllegalArgumentException(String.format("The values for timeValue and requestLimit must be positive. " +
                    "But was received — timeValue: %d, requestLimit: %d", timeValue, requestLimit));
        }
        this.requestLimit = requestLimit;
        this.requestIntervalMs = timeUnit.toMillis(timeValue);
        this.objectMapper = new ObjectMapper();
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        log.info("Client {} instanced\n", CrptApi.class.getSimpleName());
    }

    public int post(Document document, String jsonStr) {
        synchronized (lock) {
            long currentTime;
            while (true) {
                currentTime = System.currentTimeMillis();

                if (currentTime - intervalStartMs >= requestIntervalMs) {
                    intervalStartMs = currentTime;
                    requestCount = 0;
                    log.debug("Counter reset\n");
                }

                if (requestCount >= requestLimit) {
                    long waitTimeMs = requestIntervalMs - (currentTime - intervalStartMs);
                    long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(waitTimeMs);
                    log.info("Please wait for {} min {} sec {} ms (total ms: {})\n",
                            (totalSeconds / 60), (totalSeconds % 60), (waitTimeMs % 1000), waitTimeMs);
                    if (waitTimeMs > 0) {
                        try {
                            lock.wait(waitTimeMs); //Если не хотите освобождать монитор, замените на Thread.sleep(waitTimeMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } else {
                    break;
                }
            }

            int statusCode = 500;
            URI uri = URI.create(URL);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonStr))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            try {
                log.debug("Trying to send request to {}", uri);
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (requestCount == 0) {
                    intervalStartMs = currentTime;
                }
                requestCount++;

                statusCode = response.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    log.error("Request failed with status code {}\n", statusCode);
                } else {
                    log.debug("Request successful with status code {}\n", statusCode);
                }

            } catch (HttpTimeoutException e) {
                statusCode = 504;
                log.error("Request timed out: {}", e.getMessage());
            } catch (ConnectException e) {
                statusCode = 502;
                log.error("Connection failure: {}", e.getMessage());
            } catch (IOException e) {
                statusCode = 503;
                log.error("I/O Exception: {}", e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Request interrupted: {}", e.getMessage());
            } catch (Exception e) {
                log.error("An error has occurred: {}", e.getMessage());
                e.printStackTrace();
            }

            return statusCode;
        }
    }

    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Document {
        Description description;
        String docId;
        String docStatus;
        String docType;
        boolean importRequest;
        String ownerInn;
        String participantInn;
        String producerInn;
        String productionDate;
        String productionType;
        List<Product> products;
        String regDate;
        String regNumber;
    }

    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Description {
        String participantInn;
    }

    @Getter
    @Setter
    @ToString
    @Accessors(chain = true)
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Product {
        String certificateDocument;
        String certificateDocumentDate;
        String certificateDocumentNumber;
        String ownerInn;
        String producerInn;
        String productionDate;
        String tnvedCode;
        String uitCode;
        String uituCode;
    }
}