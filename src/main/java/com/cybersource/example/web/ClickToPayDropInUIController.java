package com.cybersource.example.web;

import com.cybersource.example.service.JwtProcessorService;
import com.cybersource.example.service.PaymentCredentialsService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.nio.file.Files;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
@RequiredArgsConstructor
@SessionAttributes({"usedDropInUI", "encodedTransientToken", "decodedTransientToken"})
public class ClickToPayDropInUIController {

    @Value("classpath:default-ctp-drop-in-ui-capture-context-request.json")
    private Resource ctpDropInUICaptureContextRequestJson;

    @Autowired
    private final PaymentCredentialsService paymentCredentialsService;

    @Autowired
    private final JwtProcessorService jwtProcessorService;

    @GetMapping("/ctp-drop-in-ui-overview")
    @SneakyThrows
    public String ctpDropInUIOverview(final Model model) {
        try (final Stream<String> lines = Files.lines(ctpDropInUICaptureContextRequestJson.getFile().toPath())) {
            final long lineCount = lines.count();
            model.addAttribute("requestLineCount", lineCount);
        }
        model.addAttribute("requestJson", IOUtils.toString(ctpDropInUICaptureContextRequestJson.getInputStream(), UTF_8));
        model.addAttribute("usedDropInUI", true);
        return "ctp-drop-in-ui-overview";
    }

    @PostMapping("/payment-credentials")
    public String paymentCredentials(final Model model) {
        final String encryptedCredentials = paymentCredentialsService.getEncryptedPaymentCredentials(
                model.getAttribute("decodedTransientToken").toString());
        final String decryptedCredentials = jwtProcessorService.decryptPaymentCredentials(encryptedCredentials);

        model.addAttribute("encryptedPaymentCredentials", encryptedCredentials);
        model.addAttribute("decryptedPaymentCredentials", decryptedCredentials);

        return "payment-credentials";
    }
}
