package com.kleaves.demo.service;

import com.kleaves.demo.model.Ticket;
import com.kleaves.demo.repository.TicketRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @PostConstruct
    public void seedData() {
        ticketRepository.save(new Ticket("周杰伦演唱会", 100));
        ticketRepository.save(new Ticket("漫威展", 50));
    }

    public Ticket findById(Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    /**
     * ⚠️ 无并发保护：读取→扣减→保存，非原子操作
     * 并发场景下两个线程可能读到相同库存，导致超卖
     */
    @Transactional
    public boolean book(Long ticketId, int quantity) {
        Ticket ticket = ticketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return false;
        if (ticket.getAvailableQuantity() < quantity) return false;
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() - quantity);
        ticketRepository.save(ticket);
        return true;
    }
}
