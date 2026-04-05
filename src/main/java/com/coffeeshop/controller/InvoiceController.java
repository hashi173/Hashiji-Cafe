package com.coffeeshop.controller;

import com.coffeeshop.entity.Order;
import com.coffeeshop.entity.OrderDetail;
import com.coffeeshop.entity.OrderDetailTopping;
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
    public void generateInvoice(@PathVariable Long orderId, HttpServletResponse response)
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
        Document document = new Document(PageSize.A4); // or PageSize.A5 for smaller receipts
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
        addTableHeader(table, "Size/Attr", boldFont);
        addTableHeader(table, "Qty", boldFont);
        addTableHeader(table, "Price", boldFont);

        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);

        for (OrderDetail detail : order.getOrderDetails()) {
            // Product Name + Toppings
            String itemName = detail.getProductName();
            if (detail.getSelectedToppings() != null && !detail.getSelectedToppings().isEmpty()) {
                StringBuilder sb = new StringBuilder(itemName);
                sb.append("\n + ");
                for (OrderDetailTopping t : detail.getSelectedToppings()) {
                    sb.append(t.getToppingName()).append(", ");
                }
                itemName = sb.substring(0, sb.length() - 2); // remove last comma
            }

            table.addCell(new PdfPCell(new Phrase(itemName, normalFont)));

            // Attributes / Size
            String meta = detail.getSizeSelected();
            if (detail.getAttributes() != null && !detail.getAttributes().isEmpty()) {
                meta += "\n" + detail.getAttributes();
            }
            table.addCell(new PdfPCell(new Phrase(meta, smallFont)));

            table.addCell(new PdfPCell(new Phrase(String.valueOf(detail.getQuantity()), normalFont)));
            table.addCell(new PdfPCell(
                    new Phrase(currency.format(detail.getPriceAtPurchase() * detail.getQuantity()), normalFont)));
        }

        document.add(table);

        // Total
        document.add(new Paragraph(" ", normalFont)); // spacer
        Paragraph total = new Paragraph("Total: " + currency.format(order.getTotalAmount()), titleFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

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
