package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderItem;
import com.coffeeshop.service.OrderService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public void generateInvoice(@PathVariable java.util.UUID orderId, HttpServletResponse response)
            throws IOException, DocumentException {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found");
            return;
        }

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "inline; filename=invoice_" + order.getTrackingCode() + ".pdf";
        response.setHeader(headerKey, headerValue);

        generatePdf(order, response);
    }

    private void generatePdf(Order order, HttpServletResponse response) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Header
        Paragraph title = new Paragraph("Hashiji Cafe Invoice", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        document.add(new Paragraph(" ", normalFont)); // spacer

        // Order Info
        document.add(new Paragraph("Order Code: " + order.getTrackingCode(), boldFont));
        document.add(new Paragraph(
                "Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), normalFont));
        document.add(new Paragraph(
                "Customer: " + (order.getCustomerName() != null ? order.getCustomerName() : "Walk-in"), normalFont));

        document.add(new Paragraph(" ", normalFont)); // spacer

        // Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 4, 2, 1, 2 });

        // Table Header
        addTableHeader(table, "Item", boldFont);
        addTableHeader(table, "Options", boldFont);
        addTableHeader(table, "Qty", boldFont);
        addTableHeader(table, "Price", boldFont);

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

        if (order.getOrderItems() != null) {
            for (OrderItem detail : order.getOrderItems()) {
                String itemName = detail.getSnapshotProductName() != null
                        ? detail.getSnapshotProductName()
                        : "Unknown";

                table.addCell(new PdfPCell(new Phrase(itemName, normalFont)));

                // Options
                String meta = detail.getSnapshotOptions() != null ? detail.getSnapshotOptions() : "";
                table.addCell(new PdfPCell(new Phrase(meta, smallFont)));

                table.addCell(
                        new PdfPCell(new Phrase(String.valueOf(detail.getQuantity()), normalFont)));

                double lineTotal = detail.getSnapshotUnitPrice() != null
                        ? detail.getSnapshotUnitPrice().doubleValue() * detail.getQuantity()
                        : 0.0;
                table.addCell(new PdfPCell(new Phrase(currency.format(lineTotal), normalFont)));
            }
        }

        document.add(table);

        // Total
        document.add(new Paragraph(" ", normalFont)); // spacer
        double total = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
        Paragraph totalPara = new Paragraph("Total: " + currency.format(total), titleFont);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(totalPara);

        // Footer
        document.add(new Paragraph(" ", normalFont));
        Paragraph footer = new Paragraph("Thank you for visiting Hashiji Cafe!", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }

    private void addTableHeader(PdfPTable table, String title, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }
}
