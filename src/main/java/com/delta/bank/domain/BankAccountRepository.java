package com.delta.bank.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BankAccountRepository extends JpaRepository<BankAccountEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a
        from BankAccountEntity a
        where a.number in :numbers
        order by a.number
        """)
    List<BankAccountEntity> findAllForUpdateOrderByNumber(@Param("numbers") Collection<String> numbers);
}