package com.acme.orders.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.acme.orders.config.AppProperties;
import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.repo.OrderRepository;

import org.springframework.stereotype.Service;

/**
 * File exchange with other teams. Every read and write names its charset: since JDK 18
 * the default is UTF-8 on every host (JEP 400), so relying on it silently changed the
 * bytes on machines that used to default to something else.
 */
@Service
public class ReportService {

    private final OrderRepository repository;
    private final Path reportDirectory;

    public ReportService(OrderRepository repository, AppProperties properties) {
        this.repository = repository;
        this.reportDirectory = Path.of(properties.getReportDirectory());
    }

    /** Writes the nightly CSV that the finance team's Excel macro picks up. */
    public Path writeDailyReport() throws IOException {
        Files.createDirectories(reportDirectory);
        Path out = reportDirectory.resolve("orders-daily.csv");

        try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            writer.write("id;customer;total;status\n");
            for (PurchaseOrder order : repository.findAll()) {
                writer.write(String.format(Locale.ROOT, "%d;%s;%.2f;%s%n",
                        order.getId(), order.getCustomerRef(), order.getTotalAmount(), order.getStatus()));
            }
        }
        return out;
    }

    /** Reads the SKU allow-list our merchandising team drops on the share. */
    public List<String> readSkuAllowList() throws IOException {
        Path file = reportDirectory.resolve("sku-allowlist.txt");
        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toUpperCase(Locale.ROOT))
                    .toList();
        }
    }

    public String readLegacyManifest() throws IOException {
        return Files.readString(reportDirectory.resolve("manifest.txt"), StandardCharsets.UTF_8);
    }

    public String formatCurrencyLabel(double amount) {
        return String.format(Locale.ROOT, "%,.2f", amount);
    }

    public byte[] serializeManifest(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
