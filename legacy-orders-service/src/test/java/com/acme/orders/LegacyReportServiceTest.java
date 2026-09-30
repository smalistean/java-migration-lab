package com.acme.orders;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import com.acme.orders.config.AppProperties;
import com.acme.orders.repo.OrderRepository;
import com.acme.orders.service.ReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LegacyReportServiceTest {

    @TempDir
    File folder;

    @Mock
    private OrderRepository repository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties("http://localhost:9090", null, 50, folder.getAbsolutePath());
        reportService = new ReportService(repository, props);
    }

    @Test
    void readsSkuAllowListSkippingComments() throws Exception {
        File allow = new File(folder, "sku-allowlist.txt");
        try (PrintWriter w = new PrintWriter(new FileWriter(allow))) {
            w.println("# merchandising export");
            w.println("widget-1");
            w.println("");
            w.println("café-crema");
        }

        List<String> skus = reportService.readSkuAllowList();

        assertThat(skus).containsExactly("WIDGET-1", "CAFÉ-CREMA");
    }
}
