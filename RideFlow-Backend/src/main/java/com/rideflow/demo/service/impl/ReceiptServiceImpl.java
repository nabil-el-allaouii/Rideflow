package com.rideflow.demo.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.rideflow.demo.api.dto.receipt.ReceiptResponse;
import com.rideflow.demo.api.exception.BusinessRuleException;
import com.rideflow.demo.api.exception.ResourceNotFoundException;
import com.rideflow.demo.domain.enums.PaymentStatus;
import com.rideflow.demo.domain.enums.PaymentType;
import com.rideflow.demo.domain.model.Payment;
import com.rideflow.demo.domain.model.Receipt;
import com.rideflow.demo.domain.model.Rental;
import com.rideflow.demo.domain.repository.PaymentRepository;
import com.rideflow.demo.domain.repository.ReceiptRepository;
import com.rideflow.demo.security.AuthenticatedUserService;
import com.rideflow.demo.service.ReceiptService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service
@Transactional(readOnly = true)
public class ReceiptServiceImpl implements ReceiptService {

    private static final DateTimeFormatter RECEIPT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.of("UTC"));

    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ReceiptServiceImpl(
        ReceiptRepository receiptRepository,
        PaymentRepository paymentRepository,
        AuthenticatedUserService authenticatedUserService
    ) {
        this.receiptRepository = receiptRepository;
        this.paymentRepository = paymentRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    public ReceiptResponse findByRentalId(Long rentalId) {
        Receipt receipt = receiptRepository.findByRentalId(rentalId)
            .orElseThrow(() -> new ResourceNotFoundException("Receipt not found."));
        validateAccess(receipt.rental);
        return toResponse(receipt);
    }

    @Override
    public String generateHtmlByRentalId(Long rentalId) {
        ReceiptResponse receipt = findByRentalId(rentalId);

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <title>%s</title>
              <style>
                body { font-family: Arial, sans-serif; margin: 32px; color: #17293a; }
                h1 { margin: 0 0 8px; }
                h2 { margin: 24px 0 8px; font-size: 18px; }
                .meta { color: #506676; margin-bottom: 24px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 24px; }
                .label { color: #5f7583; font-size: 12px; text-transform: uppercase; }
                .value { font-weight: 600; margin-top: 2px; }
                table { width: 100%%; border-collapse: collapse; margin-top: 12px; }
                th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #d9e4e8; }
                th { color: #5f7583; font-size: 12px; text-transform: uppercase; }
              </style>
            </head>
            <body>
              <h1>RideFlow Receipt</h1>
              <p class="meta">%s · Generated %s UTC</p>

              <h2>Ride</h2>
              <div class="grid">
                <div><div class="label">Scooter</div><div class="value">%s</div></div>
                <div><div class="label">Customer</div><div class="value">%s</div></div>
                <div><div class="label">Start</div><div class="value">%s</div></div>
                <div><div class="label">End</div><div class="value">%s</div></div>
                <div><div class="label">Duration</div><div class="value">%s</div></div>
                <div><div class="label">Distance</div><div class="value">%s km</div></div>
                <div><div class="label">Battery</div><div class="value">%s%% → %s%%</div></div>
                <div><div class="label">Battery Used</div><div class="value">%s%%</div></div>
              </div>

              <h2>Pricing</h2>
              <table>
                <thead>
                  <tr><th>Item</th><th>Amount</th><th>Status / Reference</th></tr>
                </thead>
                <tbody>
                  <tr><td>Unlock Fee</td><td>%s</td><td>%s / %s</td></tr>
                  <tr><td>Usage Cost</td><td>%s</td><td>Rate %s/min</td></tr>
                  <tr><td>Final Payment</td><td>%s</td><td>%s / %s</td></tr>
                  <tr><td>Total</td><td>%s</td><td>%s</td></tr>
                </tbody>
              </table>
            </body>
            </html>
            """.formatted(
            HtmlUtils.htmlEscape(receipt.receiptCode()),
            HtmlUtils.htmlEscape(receipt.receiptCode()),
            HtmlUtils.htmlEscape(formatInstant(receipt.generatedAt())),
            HtmlUtils.htmlEscape(receipt.scooterModel() + " · " + receipt.scooterCode()),
            HtmlUtils.htmlEscape(receipt.userFullName() + " · " + receipt.userEmail()),
            HtmlUtils.htmlEscape(formatInstant(receipt.startTime())),
            HtmlUtils.htmlEscape(formatInstant(receipt.endTime())),
            HtmlUtils.htmlEscape(receipt.durationLabel()),
            formatMoney(receipt.distanceTraveled()),
            receipt.batteryAtStart(),
            receipt.batteryAtEnd(),
            receipt.batteryConsumed(),
            formatMoney(receipt.unlockFee()),
            receipt.unlockPaymentStatus(),
            safeString(receipt.unlockPaymentReference()),
            formatMoney(receipt.usageCost()),
            formatMoney(receipt.ratePerMinute()),
            formatMoney(receipt.totalCost().subtract(receipt.unlockFee()).max(BigDecimal.ZERO)),
            receipt.finalPaymentStatus() == null ? "N/A" : receipt.finalPaymentStatus(),
            safeString(receipt.finalPaymentReference()),
            formatMoney(receipt.totalCost()),
            safeString(receipt.finalPaymentFailureReason())
        );
    }

    @Override
    public byte[] generatePdfByRentalId(Long rentalId) {
        ReceiptResponse receipt = findByRentalId(rentalId);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();
            document.add(new Paragraph("RideFlow Receipt", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph(receipt.receiptCode()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Scooter: " + receipt.scooterModel() + " · " + receipt.scooterCode()));
            document.add(new Paragraph("Customer: " + receipt.userFullName() + " · " + receipt.userEmail()));
            document.add(new Paragraph("Start: " + formatInstant(receipt.startTime())));
            document.add(new Paragraph("End: " + formatInstant(receipt.endTime())));
            document.add(new Paragraph("Duration: " + receipt.durationLabel()));
            document.add(new Paragraph("Distance: " + formatMoney(receipt.distanceTraveled()) + " km"));
            document.add(new Paragraph(
                "Battery: "
                    + receipt.batteryAtStart()
                    + "% -> "
                    + receipt.batteryAtEnd()
                    + "% (used "
                    + receipt.batteryConsumed()
                    + "%)"
            ));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Unlock Fee: " + formatMoney(receipt.unlockFee())));
            document.add(new Paragraph("Usage Cost: " + formatMoney(receipt.usageCost())));
            document.add(new Paragraph("Rate Per Minute: " + formatMoney(receipt.ratePerMinute())));
            document.add(new Paragraph("Total Cost: " + formatMoney(receipt.totalCost())));
            document.add(new Paragraph("Unlock Payment Ref: " + safeString(receipt.unlockPaymentReference())));
            document.add(new Paragraph("Final Payment Ref: " + safeString(receipt.finalPaymentReference())));
            if (receipt.finalPaymentFailureReason() != null) {
                document.add(new Paragraph("Final Payment Failure: " + receipt.finalPaymentFailureReason()));
            }
            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("Unable to generate receipt PDF.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to render receipt PDF.", exception);
        }
    }

    private ReceiptResponse toResponse(Receipt receipt) {
        Rental rental = receipt.rental;
        Payment unlockPayment = paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(rental.id, PaymentType.UNLOCK_FEE).orElse(null);
        Payment finalPayment = paymentRepository.findFirstByRentalIdAndTypeOrderByCreatedAtDesc(rental.id, PaymentType.FINAL_PAYMENT).orElse(null);
        BigDecimal usageCost = receipt.usageCost == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : receipt.usageCost;
        Integer batteryStart = rental.batteryAtStart == null ? 0 : rental.batteryAtStart;
        Integer batteryEnd = rental.batteryAtEnd == null ? batteryStart : rental.batteryAtEnd;

        return new ReceiptResponse(
            receipt.id,
            receipt.receiptCode,
            rental.id,
            rental.scooter.publicCode,
            rental.scooter.model,
            rental.user.fullName,
            rental.user.email,
            rental.startTime,
            rental.endTime,
            rental.durationMinutes,
            formatDurationLabel(rental.durationMinutes),
            defaultMoney(receipt.unlockFeeCharged),
            defaultMoney(rental.ratePerMinuteApplied),
            defaultMoney(usageCost),
            defaultMoney(receipt.totalCost),
            batteryStart,
            batteryEnd,
            Math.max(0, batteryStart - batteryEnd),
            defaultMoney(rental.distanceTraveled),
            unlockPayment == null ? null : unlockPayment.status,
            unlockPayment == null ? null : unlockPayment.transactionReference,
            finalPayment == null ? null : finalPayment.status,
            finalPayment == null ? null : finalPayment.transactionReference,
            finalPayment == null ? null : finalPayment.failureReason,
            receipt.generatedAt
        );
    }

    private void validateAccess(Rental rental) {
        if (authenticatedUserService.isAdmin()) {
            return;
        }

        Long currentUserId = authenticatedUserService.getCurrentUserId();
        if (!currentUserId.equals(rental.user.id)) {
            throw new BusinessRuleException("You can only access your own receipts.");
        }
    }

    private String formatDurationLabel(Integer durationMinutes) {
        int minutes = durationMinutes == null ? 0 : Math.max(durationMinutes, 0);
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours == 0) {
            return remainingMinutes + " min";
        }

        if (remainingMinutes == 0) {
            return hours + " h";
        }

        return hours + " h " + remainingMinutes + " min";
    }

    private String formatInstant(Instant value) {
        return value == null ? "N/A" : RECEIPT_DATE_FORMATTER.format(value);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatMoney(BigDecimal value) {
        return defaultMoney(value).toPlainString();
    }

    private String safeString(String value) {
        return value == null || value.isBlank() ? "N/A" : HtmlUtils.htmlEscape(value);
    }
}
