package com.acme.orders;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import com.acme.orders.config.AppProperties;
import com.acme.orders.repo.OrderRepository;
import com.acme.orders.service.ReportService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Still on JUnit 4 because of the TemporaryFolder rule. */
@RunWith(MockitoJUnitRunner.class)
public class LegacyReportServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private OrderRepository repository;

    private ReportService reportService;

    @Before
    public void setUp() throws Exception {
        AppProperties props = new AppProperties(
                "http://localhost:9090", null, 50, folder.getRoot().getAbsolutePath());
        reportService = new ReportService(repository, props);
    }

    @Test
    public void readsSkuAllowListSkippingComments() throws Exception {
        File allow = new File(folder.getRoot(), "sku-allowlist.txt");
        try (PrintWriter w = new PrintWriter(new FileWriter(allow))) {
            w.println("# merchandising export");
            w.println("widget-1");
            w.println("");
            w.println("café-crema");
        }

        List<String> skus = reportService.readSkuAllowList();

        assertEquals(2, skus.size());
        assertTrue(skus.contains("WIDGET-1"));
        assertTrue(skus.contains("CAFÉ-CREMA"));
    }
}
