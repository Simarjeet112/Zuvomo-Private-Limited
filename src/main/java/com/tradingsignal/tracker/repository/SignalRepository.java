package com.tradingsignal.tracker.repository;

import com.tradingsignal.tracker.entity.Signal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignalRepository extends JpaRepository<Signal, Long> {
    // JpaRepository already gives us save(), findAll(), findById(), deleteById().
    // No custom queries needed yet for the minimum endpoints in the PDF.
}