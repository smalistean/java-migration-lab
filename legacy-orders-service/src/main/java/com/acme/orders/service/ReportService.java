package com.acme.orders.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.acme.orders.config.AppProperties;
import com.acme.orders.domain.PurchaseOrder;
import com.acme.orders.repo.OrderRepository;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final OrderRepository repository;
    private final AppProperties properties;

    public ReportService(OrderRepository repository, AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Writes the nightly CSV that the finance team's Excel macro picks up. */
    public File writeDailyReport() throws IOException {
        File dir = new File(properties.getReportDirectory());
        dir.mkdirs();
        File out = new File(dir, "orders-daily.csv");

        try (PrintWriter writer = new PrintWriter(new FileWriter(out))) {
            writer.println("id;customer;total;status");
            for (PurchaseOrder order : repository.findAll()) {
                writer.printf("%d;%s;%s;%s%n",
                        order.getId(),
                        order.getCustomerRef(),
                        String.format("%.2f", order.getTotalAmount()),
                        order.getStatus());
            }
        }
        return out;
    }

    /** Reads the SKU allow-list our merchandising team drops on the share. */
    public List<String> readSkuAllowList() throws IOException {
        File file = new File(properties.getReportDirectory(), "sku-allowlist.txt");
        List<String> skus = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    skus.add(line.trim().toUpperCase());
                }
            }
        }
        return skus;
    }

    public String readLegacyManifest() throws IOException {
        File manifest = new File(properties.getReportDirectory(), "manifest.txt");
        return FileUtils.readFileToString(manifest);
    }

    public String formatCurrencyLabel(double amount) {
        return String.format(Locale.getDefault(), "%,.2f", amount);
    }

    public byte[] serializeManifest(String content) {
        return content.getBytes();
    }

    public List<String> readAllLines(File f) throws IOException {
        return Files.readAllLines(f.toPath());
    }
}
