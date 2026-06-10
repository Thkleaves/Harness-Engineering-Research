package com.kleaves.demo.controller;

import com.kleaves.demo.model.Ticket;
import com.kleaves.demo.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getById(@PathVariable Long id) {
        Ticket ticket = ticketService.findById(id);
        return ticket != null ? ResponseEntity.ok(ticket) : ResponseEntity.notFound().build();
    }

    /** ⚠️ 无并发保护 — 超卖风险 */
    @PostMapping("/{id}/book")
    public ResponseEntity<Map<String, Object>> book(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int quantity) {
        boolean success = ticketService.book(id, quantity);
        if (success) {
            Ticket ticket = ticketService.findById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "remaining", ticket.getAvailableQuantity()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "reason", "库存不足或票据不存在"
        ));
    }
}
