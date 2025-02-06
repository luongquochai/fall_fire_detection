package com.camai.fall_fire_detection.service;

import com.camai.fall_fire_detection.model.Event;
import com.camai.fall_fire_detection.model.EventKey;
import com.camai.fall_fire_detection.model.EventType;
import com.camai.fall_fire_detection.model.EventStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class EventServicePerformanceTest {
    private final Logger logger = LoggerFactory.getLogger(EventServicePerformanceTest.class);
    private List<TestResult> testResults = new ArrayList<>();

    @Autowired
    private EventService eventService;

    // Thêm class để lưu kết quả test
    private static class TestResult {
        int numberOfRequests;
        long totalTime;
        double averageTimePerRequest;
        double requestsPerSecond;

        public TestResult(int numberOfRequests, long totalTime) {
            this.numberOfRequests = numberOfRequests;
            this.totalTime = totalTime;
            this.averageTimePerRequest = (double) totalTime / numberOfRequests;
            this.requestsPerSecond = (1000.0 * numberOfRequests) / totalTime;
        }
    }

    @Test
    public void testBulkEventCreation() {
        runBulkTest(100);
        runBulkTest(1000);
        runBulkTest(10000);
        
        generateHtmlReport();
    }

    private void runBulkTest(int numberOfRequests) {
        logger.info("Starting bulk test with {} requests", numberOfRequests);
        
        List<Event> events = generateEvents(numberOfRequests);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();

        // Gửi các request bất đồng bộ
        for (Event event : events) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    eventService.saveEvent(event);
                } catch (Exception e) {
                    logger.error("Error saving event: ", e);
                }
            }, executor);
            futures.add(future);
        }

        // Đợi tất cả các request hoàn thành
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        TestResult result = new TestResult(numberOfRequests, duration);
        testResults.add(result);
        
        logger.info("Test results for {} requests:", numberOfRequests);
        logger.info("Total time: {} ms", duration);
        logger.info("Average time per request: {} ms", (double) duration / numberOfRequests);
        logger.info("Requests per second: {}", (1000.0 * numberOfRequests) / duration);
        
        executor.shutdown();
    }

    private void generateHtmlReport() {
        try {
            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Performance Test Results</title>
                    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                    <style>
                        .chart-container { width: 800px; margin: 20px auto; }
                    </style>
                </head>
                <body>
                    <div class="chart-container">
                        <canvas id="timeChart"></canvas>
                    </div>
                    <div class="chart-container">
                        <canvas id="rpsChart"></canvas>
                    </div>
                    <script>
                        const requestCounts = %s;
                        const totalTimes = %s;
                        const avgTimes = %s;
                        const rps = %s;
                        
                        new Chart(document.getElementById('timeChart'), {
                            type: 'bar',
                            data: {
                                labels: requestCounts,
                                datasets: [{
                                    label: 'Total Time (ms)',
                                    data: totalTimes,
                                    backgroundColor: 'rgba(54, 162, 235, 0.5)'
                                }, {
                                    label: 'Avg Time per Request (ms)',
                                    data: avgTimes,
                                    backgroundColor: 'rgba(255, 99, 132, 0.5)'
                                }]
                            },
                            options: {
                                responsive: true,
                                title: { display: true, text: 'Time Metrics' }
                            }
                        });
                        
                        new Chart(document.getElementById('rpsChart'), {
                            type: 'line',
                            data: {
                                labels: requestCounts,
                                datasets: [{
                                    label: 'Requests per Second',
                                    data: rps,
                                    borderColor: 'rgb(75, 192, 192)',
                                    tension: 0.1
                                }]
                            },
                            options: {
                                responsive: true,
                                title: { display: true, text: 'Throughput' }
                            }
                        });
                    </script>
                </body>
                </html>
            """.formatted(
                testResults.stream().map(r -> r.numberOfRequests).toList(),
                testResults.stream().map(r -> r.totalTime).toList(),
                testResults.stream().map(r -> r.averageTimePerRequest).toList(),
                testResults.stream().map(r -> r.requestsPerSecond).toList()
            );

            String reportPath = "target/performance-report.html";
            Files.writeString(Path.of(reportPath), htmlContent);
            logger.info("Performance report generated at: {}", reportPath);
        } catch (IOException e) {
            logger.error("Error generating report: ", e);
        }
    }

    private List<Event> generateEvents(int count) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Event event = new Event();
            EventKey key = new EventKey();
            
            String messageId = UUID.randomUUID().toString();
            
            // Set EventKey
            key.setUserId("test-user-" + (i % 10)); // Giả lập 10 user khác nhau
            key.setMessageId(messageId);
            key.setDeviceId("device-" + (i % 5)); // Giả lập 5 device khác nhau
            key.setEventType(EventType.FALL); // hoặc random giữa FALL và FIRE
            key.setEventDate(Instant.now());
            key.setBucket(1);
            key.setCategory(1);
            
            // Set Event
            event.setKey(key);
            event.setDeviceId(key.getDeviceId());
            event.setEventType(key.getEventType());
            event.setDescription("Test event " + i);
            event.setThumbnailUrl("http://example.com/thumb" + i);
            event.setCreatedAt(Instant.now());
            event.setStatus(EventStatus.PENDING);
            event.setMessageId(messageId);
            
            events.add(event);
        }
        return events;
    }
} 