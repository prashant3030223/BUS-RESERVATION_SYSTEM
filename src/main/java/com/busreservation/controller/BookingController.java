package com.busreservation.controller;

import com.busreservation.exception.ResourceNotFoundException;
import com.busreservation.model.*;
import com.busreservation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.Set;

@Controller
public class BookingController {

        @Autowired
        private ScheduleService scheduleService;
        @Autowired
        private BookingService bookingService;
        @Autowired
        private UserService userService;
        @Autowired
        private LoyaltyService loyaltyService;
        @Autowired
        private TransactionService transactionService;

        @GetMapping("/book/{scheduleId}")
        public String showBookingPage(@PathVariable Long scheduleId, Model model, Authentication authentication) {
                Schedule schedule = scheduleService.getScheduleById(scheduleId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Schedule not found with id: " + scheduleId));

                Set<String> bookedSeats = scheduleService.getBookedSeats(scheduleId);

                // Get user's loyalty points
                User currentUser = userService.findByEmail(authentication.getName());
                int totalPoints = loyaltyService.getTotalPoints(currentUser);
                double availableDiscount = loyaltyService.calculateDiscount(totalPoints);

                model.addAttribute("schedule", schedule);
                model.addAttribute("bookedSeats", bookedSeats);
                model.addAttribute("booking", new Booking());
                model.addAttribute("totalPoints", totalPoints);
                model.addAttribute("availableDiscount", availableDiscount);
                return "booking-page";
        }

        @PostMapping("/book/confirm")
        public String confirmBooking(@ModelAttribute Booking booking,
                        @RequestParam Long scheduleId,
                        @RequestParam(required = false, defaultValue = "0") int pointsToRedeem,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes) {
                User currentUser = userService.findByEmail(authentication.getName());
                Schedule schedule = scheduleService.getScheduleById(scheduleId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Schedule not found with id: " + scheduleId));

                String[] selectedSeats = booking.getSeatNumbers().split(",");
                int numberOfSeats = selectedSeats.length;

                // Check if sufficient seats available
                if (schedule.getAvailableSeats() < numberOfSeats) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                        "Sorry, only " + schedule.getAvailableSeats() + " seat(s) available.");
                        return "redirect:/book/" + scheduleId;
                }

                // Calculate price
                double pricePerSeat = schedule.getPrice();
                double totalPrice = numberOfSeats * pricePerSeat;

                // Apply loyalty points discount
                double discount = 0;
                if (pointsToRedeem > 0) {
                        int userPoints = loyaltyService.getTotalPoints(currentUser);
                        if (pointsToRedeem <= userPoints) {
                                discount = loyaltyService.calculateDiscount(pointsToRedeem);
                                totalPrice = Math.max(0, totalPrice - discount);

                                // Redeem points
                                loyaltyService.redeemPoints(currentUser, pointsToRedeem,
                                                "Redeemed for booking #" + booking.getId());
                        }
                }

                booking.setTotalPrice(totalPrice);

                // Update available seats
                schedule.setAvailableSeats(schedule.getAvailableSeats() - numberOfSeats);
                scheduleService.saveSchedule(schedule);

                // Save booking
                booking.setUser(currentUser);
                booking.setSchedule(schedule);
                booking.setBookingDate(LocalDate.now());
                booking.setStatus("CONFIRMED");

                Booking savedBooking = bookingService.saveBooking(booking);

                // Award loyalty points is handled in BookingService

                // Check if user was referred and award referral bonus
                loyaltyService.getUserReferrals(currentUser).stream()
                                .filter(r -> r.getReferredUser() != null
                                                && r.getReferredUser().getId().equals(currentUser.getId()))
                                .filter(r -> "COMPLETED".equals(r.getStatus()))
                                .findFirst()
                                .ifPresent(referral -> {
                                        loyaltyService.awardReferralBonus(referral.getReferrer(), currentUser);
                                });

                // Record Transaction
                transactionService.recordPayment(savedBooking, savedBooking.getTotalPrice(), "Booking Confirmation");

                redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed successfully!");
                return "redirect:/ticket/" + savedBooking.getId();
        }

        @GetMapping("/ticket/{bookingId}")
        public String showTicketPage(@PathVariable Long bookingId, Model model, Authentication authentication) {
                Booking booking = bookingService.getBookingById(bookingId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Booking not found with id: " + bookingId));

                User currentUser = userService.findByEmail(authentication.getName());

                // Security Check: Allow if user owns the booking OR is ADMIN
                if (!booking.getUser().getId().equals(currentUser.getId())
                                && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                        return "redirect:/my-bookings?error=unauthorized";
                }

                model.addAttribute("booking", booking);
                return "ticket";
        }

        @GetMapping("/my-bookings")
        public String showMyBookings(Model model, Authentication authentication) {
                User currentUser = userService.findByEmail(authentication.getName());
                model.addAttribute("bookings", bookingService.getBookingsByUser(currentUser));
                return "my-bookings";
        }

        @PostMapping("/booking/cancel/{id}")
        public String cancelBooking(@PathVariable Long id, Authentication authentication) {
                User currentUser = userService.findByEmail(authentication.getName());
                Booking booking = bookingService.getBookingById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

                if (!booking.getUser().getId().equals(currentUser.getId())) {
                        return "redirect:/my-bookings?error";
                }

                if ("CONFIRMED".equals(booking.getStatus())) {
                        booking.setStatus("CANCELLED");
                        bookingService.saveBooking(booking);

                        // Seats ki ginti wapas badhayein
                        Schedule schedule = booking.getSchedule();
                        int cancelledSeatsCount = booking.getSeatNumbers().split(",").length;
                        schedule.setAvailableSeats(schedule.getAvailableSeats() + cancelledSeatsCount);
                        scheduleService.saveSchedule(schedule);

                        // Record Refund
                        transactionService.recordRefund(booking, booking.getTotalPrice(),
                                        "Booking Cancellation Refund");
                } else {
                        booking.setStatus("CANCELLED");
                        bookingService.saveBooking(booking);
                }
                return "redirect:/my-bookings?cancel_success";
        }

        @GetMapping("/booking/reschedule/{id}")
        public String rescheduleBooking(@PathVariable Long id, @RequestParam String source,
                        @RequestParam String destination, Authentication authentication) {
                User currentUser = userService.findByEmail(authentication.getName());
                Booking booking = bookingService.getBookingById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

                if (!booking.getUser().getId().equals(currentUser.getId())) {
                        return "redirect:/my-bookings?error";
                }

                // Cancel the current booking
                if ("CONFIRMED".equals(booking.getStatus())) {
                        booking.setStatus("CANCELLED");
                        bookingService.saveBooking(booking);

                        Schedule schedule = booking.getSchedule();
                        int cancelledSeatsCount = booking.getSeatNumbers().split(",").length;
                        schedule.setAvailableSeats(schedule.getAvailableSeats() + cancelledSeatsCount);
                        scheduleService.saveSchedule(schedule);
                }

                // Redirect to search page
                return "redirect:/search?source=" + source + "&destination=" + destination;
        }

        @GetMapping("/booking/invoice/{id}")
        public void downloadInvoice(@PathVariable Long id, HttpServletResponse response,
                        Authentication authentication) {
                try {
                        User currentUser = userService.findByEmail(authentication.getName());
                        Booking booking = bookingService.getBookingById(id)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Booking not found with id: " + id));

                        if (!booking.getUser().getId().equals(currentUser.getId())
                                        && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized access to invoice.");
                                return;
                        }

                        response.setContentType("application/pdf");
                        String headerKey = "Content-Disposition";
                        String headerValue = "attachment; filename=Invoice_" + booking.getId() + ".pdf";
                        response.setHeader(headerKey, headerValue);

                        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
                        com.itextpdf.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());

                        document.open();

                        // Fonts
                        com.itextpdf.text.Font titleFont = com.itextpdf.text.FontFactory
                                        .getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 20,
                                                        com.itextpdf.text.BaseColor.BLACK);
                        com.itextpdf.text.Font subTitleFont = com.itextpdf.text.FontFactory
                                        .getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 14,
                                                        com.itextpdf.text.BaseColor.BLACK);
                        com.itextpdf.text.Font boldFont = com.itextpdf.text.FontFactory
                                        .getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10,
                                                        com.itextpdf.text.BaseColor.BLACK);
                        com.itextpdf.text.Font normalFont = com.itextpdf.text.FontFactory
                                        .getFont(com.itextpdf.text.FontFactory.HELVETICA, 10,
                                                        com.itextpdf.text.BaseColor.BLACK);
                        com.itextpdf.text.Font smallFont = com.itextpdf.text.FontFactory
                                        .getFont(com.itextpdf.text.FontFactory.HELVETICA, 8,
                                                        com.itextpdf.text.BaseColor.GRAY);

                        // --- Header Section ---
                        com.itextpdf.text.pdf.PdfPTable headerTable = new com.itextpdf.text.pdf.PdfPTable(2);
                        headerTable.setWidthPercentage(100);
                        headerTable.setWidths(new float[] { 1, 1 });

                        // Logo / Company Name
                        com.itextpdf.text.pdf.PdfPCell logoCell = new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase("TourNtravels", titleFont));
                        logoCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        logoCell.setVerticalAlignment(com.itextpdf.text.Element.ALIGN_MIDDLE);
                        headerTable.addCell(logoCell);

                        // Invoice Label
                        com.itextpdf.text.pdf.PdfPCell labelCell = new com.itextpdf.text.pdf.PdfPCell();
                        labelCell.addElement(new com.itextpdf.text.Paragraph("Tax Invoice/Bill of Supply/Cash Memo",
                                        subTitleFont));
                        labelCell.addElement(new com.itextpdf.text.Paragraph("(Original for Recipient)", smallFont));
                        labelCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        labelCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        headerTable.addCell(labelCell);

                        document.add(headerTable);
                        document.add(new com.itextpdf.text.Paragraph("\n"));

                        // --- Sold By & Billing Address ---
                        com.itextpdf.text.pdf.PdfPTable addressTable = new com.itextpdf.text.pdf.PdfPTable(2);
                        addressTable.setWidthPercentage(100);
                        addressTable.setSpacingBefore(10f);
                        addressTable.setSpacingAfter(10f);

                        // Sold By
                        com.itextpdf.text.pdf.PdfPCell soldByCell = new com.itextpdf.text.pdf.PdfPCell();
                        soldByCell.addElement(new com.itextpdf.text.Paragraph("Sold By:", boldFont));
                        soldByCell.addElement(new com.itextpdf.text.Paragraph("TourNtravels Pvt. Ltd.", normalFont));
                        soldByCell.addElement(
                                        new com.itextpdf.text.Paragraph("123, Tech Park, Bangalore, India - 560001",
                                                        normalFont));
                        soldByCell.addElement(new com.itextpdf.text.Paragraph("PAN No: ABCDE1234F", normalFont));
                        soldByCell.addElement(new com.itextpdf.text.Paragraph("GST Registration No: 29ABCDE1234F1Z5",
                                        normalFont));
                        soldByCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        addressTable.addCell(soldByCell);

                        // Billing Address
                        com.itextpdf.text.pdf.PdfPCell billingCell = new com.itextpdf.text.pdf.PdfPCell();
                        billingCell.addElement(new com.itextpdf.text.Paragraph("Billing Address:", boldFont));
                        billingCell.addElement(
                                        new com.itextpdf.text.Paragraph(booking.getUser().getFullName(), normalFont));
                        billingCell.addElement(new com.itextpdf.text.Paragraph(
                                        booking.getUser().getAddress() != null ? booking.getUser().getAddress()
                                                        : "Address not provided",
                                        normalFont));
                        billingCell
                                        .addElement(new com.itextpdf.text.Paragraph(
                                                        "Email: " + booking.getUser().getEmail(), normalFont));
                        billingCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        billingCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        addressTable.addCell(billingCell);

                        document.add(addressTable);

                        // --- Order Details ---
                        com.itextpdf.text.pdf.PdfPTable orderTable = new com.itextpdf.text.pdf.PdfPTable(2);
                        orderTable.setWidthPercentage(100);
                        orderTable.setSpacingAfter(10f);

                        com.itextpdf.text.pdf.PdfPCell orderInfoCell = new com.itextpdf.text.pdf.PdfPCell();
                        orderInfoCell.addElement(new com.itextpdf.text.Paragraph("Order Number: #" + booking.getId(),
                                        normalFont));
                        orderInfoCell.addElement(new com.itextpdf.text.Paragraph(
                                        "Order Date: " + (booking.getBookingDate() != null ? booking.getBookingDate()
                                                        : "N/A"),
                                        normalFont));
                        orderInfoCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        orderTable.addCell(orderInfoCell);

                        com.itextpdf.text.pdf.PdfPCell invoiceInfoCell = new com.itextpdf.text.pdf.PdfPCell();
                        invoiceInfoCell.addElement(new com.itextpdf.text.Paragraph(
                                        "Invoice Number: INV-" + booking.getId() + "-"
                                                        + java.time.Year.now().getValue(),
                                        normalFont));
                        invoiceInfoCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        invoiceInfoCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        orderTable.addCell(invoiceInfoCell);

                        document.add(orderTable);

                        // --- Item Table ---
                        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(5);
                        table.setWidthPercentage(100);
                        table.setWidths(new float[] { 1, 4, 2, 1, 2 });
                        table.setHeaderRows(1);

                        // Headers
                        String[] headers = { "Sl. No", "Description", "Unit Price", "Qty", "Net Amount" };
                        for (String header : headers) {
                                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                                                new com.itextpdf.text.Phrase(header, boldFont));
                                cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
                                cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                                cell.setPadding(5);
                                table.addCell(cell);
                        }

                        // Data Row
                        String busType = booking.getSchedule().getBus().getBusType();
                        if (busType == null)
                                busType = "AC Seater";

                        String description = "Bus Ticket - " + booking.getSchedule().getBus().getBusName() + " ("
                                        + busType + ")\n"
                                        +
                                        "Route: " + booking.getSchedule().getSource() + " to "
                                        + booking.getSchedule().getDestination()
                                        + "\n" +
                                        "Travel Date: " + booking.getSchedule().getDepartureTime() + "\n" +
                                        "Seats: " + booking.getSeatNumbers();

                        double totalPrice = booking.getTotalPrice();
                        int qty = booking.getSeatNumbers().split(",").length;
                        double unitPrice = totalPrice / qty;

                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase("1", normalFont)));
                        table.addCell(new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase(description, normalFont)));

                        com.itextpdf.text.pdf.PdfPCell priceCell = new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase("₹" + String.format("%.2f", unitPrice),
                                                        normalFont));
                        priceCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        table.addCell(priceCell);

                        com.itextpdf.text.pdf.PdfPCell qtyCell = new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase(String.valueOf(qty), normalFont));
                        qtyCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                        table.addCell(qtyCell);

                        com.itextpdf.text.pdf.PdfPCell totalCell = new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase("₹" + String.format("%.2f", totalPrice),
                                                        normalFont));
                        totalCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        table.addCell(totalCell);

                        document.add(table);

                        // --- Total Section ---
                        com.itextpdf.text.pdf.PdfPTable totalTable = new com.itextpdf.text.pdf.PdfPTable(2);
                        totalTable.setWidthPercentage(100);
                        totalTable.setWidths(new float[] { 3, 1 });
                        totalTable.setSpacingBefore(5f);

                        com.itextpdf.text.pdf.PdfPCell emptyCell = new com.itextpdf.text.pdf.PdfPCell();
                        emptyCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        totalTable.addCell(emptyCell);

                        com.itextpdf.text.pdf.PdfPCell grandTotalCell = new com.itextpdf.text.pdf.PdfPCell(
                                        new com.itextpdf.text.Phrase(
                                                        "Grand Total: ₹" + String.format("%.2f", totalPrice),
                                                        titleFont));
                        grandTotalCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                        grandTotalCell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        totalTable.addCell(grandTotalCell);

                        document.add(totalTable);

                        document.add(new com.itextpdf.text.Paragraph("\n"));
                        document.add(new com.itextpdf.text.Paragraph(
                                        "Amount in Words: " + convertToIndianCurrency(totalPrice),
                                        boldFont));

                        // --- Footer ---
                        document.add(new com.itextpdf.text.Paragraph("\n\n"));
                        com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                                        "For TourNtravels Pvt. Ltd.\n\n\nAuthorized Signatory", normalFont);
                        footer.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
                        document.add(footer);

                        document.close();
                } catch (Exception e) {
                        e.printStackTrace();
                        try {
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Error generating invoice: " + e.getMessage());
                        } catch (Exception ex) {
                                // Ignore
                        }
                }
        }

        private String convertToIndianCurrency(double amount) {
                // Simple placeholder for number to words conversion
                return "Rupees " + (int) amount + " Only";
        }
}