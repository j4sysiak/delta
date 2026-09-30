package com.delta.bank.lab.java21.clock

import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ClockBasedAuditServiceSpec extends Specification {

    def "records operation at the exact time provided by a fixed clock"() {
        given:
        def instant = Instant.parse("2026-09-19T12:30:00Z")
        def clock = Clock.fixed(instant, ZoneOffset.UTC)
        def service = new ClockBasedAuditService(clock)

        when:
        def audit = service.audit("PLN-1001", "DEPOSIT")

        then:
        audit.accountNumber() == "PLN-1001"
        audit.operation() == "DEPOSIT"
        audit.occurredAt() == LocalDateTime.of(2026, 9, 19, 12, 30)
    }

    def "returns the same timestamp on repeated calls when using a fixed clock"() {
        given:
        def clock = Clock.fixed(
                Instant.parse("2026-09-19T12:30:00Z"),
                ZoneOffset.UTC
        )
        def service = new ClockBasedAuditService(clock)

        when:
        def firstAudit = service.audit("PLN-1001", "DEPOSIT")
        def secondAudit = service.audit("PLN-1001", "WITHDRAW")

        then:
        firstAudit.occurredAt() == secondAudit.occurredAt()
    }

    def "uses the time zone configured on the clock"() {
        given:
        def clock = Clock.fixed(
                Instant.parse("2026-09-19T12:30:00Z"),
                ZoneOffset.ofHours(2)
        )
        def service = new ClockBasedAuditService(clock)

        when:
        def audit = service.audit("PLN-1001", "TRANSFER")

        then:
        audit.occurredAt() == LocalDateTime.of(2026, 9, 19, 14, 30)
    }

    def "can test a shifted clock without waiting"() {
        given:
        def baseClock = Clock.fixed(
                Instant.parse("2026-09-19T12:30:00Z"),
                ZoneOffset.UTC
        )
        def shiftedClock = Clock.offset(baseClock, java.time.Duration.ofMinutes(30))
        def service = new ClockBasedAuditService(shiftedClock)

        when:
        def audit = service.audit("PLN-1001", "WITHDRAW")

        then:
        audit.occurredAt() == LocalDateTime.of(2026, 9, 19, 13, 0)
    }

    def "rejects a null clock"() {
        when:
        new ClockBasedAuditService(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "clock must not be null"
    }
}