package com.acme.orders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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
class ReportServiceTest {

    @TempDir
    Path folder;

    @Mock
    private OrderRepository repository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties("http://localhost:9090", null, 50, folder.toString());
        reportService = new ReportService(repository, props);
    }

    @Test
    void readsSkuAllowListSkippingComments() throws Exception {
        Files.write(folder.resolve("sku-allowlist.txt"),
                List.of("# merchandising export", "widget-1", "", "café-crema"),
                StandardCharsets.UTF_8);

        List<String> skus = reportService.readSkuAllowList();

        assertThat(skus).containsExactly("WIDGET-1", "CAFÉ-CREMA");
    }

    /** A Latin-1 consumer would see mojibake here; the bytes on disk must be UTF-8 on every host. */
    @Test
    void manifestBytesAreUtf8RegardlessOfHost() {
        assertThat(reportService.serializeManifest("café")).containsExactly(0x63, 0x61, 0x66, 0xC3, 0xA9);
    }

    /** Must not depend on the default locale: under tr-TR, "i" upper-cases to a dotted capital. */
    @Test
    void upperCasingIsLocaleIndependent() throws Exception {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            Files.writeString(folder.resolve("sku-allowlist.txt"), "widget-1\n", StandardCharsets.UTF_8);
            assertThat(reportService.readSkuAllowList()).containsExactly("WIDGET-1");
        } finally {
            Locale.setDefault(original);
        }
    }
}
