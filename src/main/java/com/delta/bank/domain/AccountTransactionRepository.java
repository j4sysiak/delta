package com.delta.bank.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AccountTransactionRepository extends JpaRepository<AccountTransactionEntity, Long> {

    /*
    Nie ma jej w kodzie źródłowym.
    To jest metoda w interfejsie Spring Data JPA w pliku `src/main/java/com/delta/bank/domain/AccountTransactionRepository.java`,
    a jej implementację Spring generuje automatycznie w czasie uruchamiania aplikacji.

    Spring odczytuje nazwę metody:
     - `findByAccountNumber` \- filtr po `accountNumber`
     - `OrderByCreatedAtDesc` \- sortowanie malejąco po `createdAt`
     - `Pageable` \- obsługa stronicowania

    W praktyce Spring tworzy proxy dla repozytorium i wykonuje zapytanie równoważne mniej więcej temu:

        ```sql
        select \*
        from account_transaction
        where account_number = ?
        order by created_at desc
        ```

    z dołożonym `limit` / `offset` wynikającym z `Pageable`.

    Czyli:
     - ciała metody nie ma w tym pliku
     - implementacja powstaje dynamicznie przez Spring Data JPA
    * */
    Page<AccountTransactionEntity> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    java.util.Optional<AccountTransactionEntity> findFirstByTransferRequestIdAndAccountNumberAndType(String transferRequestId,
                                                                                                     String accountNumber,
                                                                                                     TransactionType type);

    Page<AccountTransactionEntity> findByAccountNumberAndTypeOrderByCreatedAtDesc(String accountNumber,
                                                                                  TransactionType type,
                                                                                  Pageable pageable);

    Page<AccountTransactionEntity> findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtDesc(String accountNumber,
                                                                                              LocalDateTime from,
                                                                                              LocalDateTime to,
                                                                                              Pageable pageable);

    Page<AccountTransactionEntity> findByAccountNumberAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(String accountNumber,
                                                                                                     TransactionType type,
                                                                                                     LocalDateTime fromDate,
                                                                                                     LocalDateTime toDate,
                                                                                                     Pageable pageable);

    Page<AccountTransactionEntity> findByAccountNumberAndAmountBetweenOrderByCreatedAtDesc(String accountNumber,
                                                                                           BigDecimal minAmount,
                                                                                           BigDecimal maxAmount,
                                                                                           Pageable pageable
    );

    Page<AccountTransactionEntity> findByAccountNumberAndTypeAndAmountBetweenOrderByCreatedAtDesc(String accountNumber,
                                                                                                  TransactionType type,
                                                                                                  BigDecimal minAmount,
                                                                                                  BigDecimal maxAmount,
                                                                                                  Pageable pageable
    );

    Page<AccountTransactionEntity> findByAccountNumberAndCreatedAtBetweenAndAmountBetweenOrderByCreatedAtDesc(String accountNumber,
                                                                                                              LocalDateTime from,
                                                                                                              LocalDateTime to,
                                                                                                              BigDecimal minAmount,
                                                                                                              BigDecimal maxAmount,
                                                                                                              Pageable pageable
    );

    Page<AccountTransactionEntity> findByAccountNumberAndTypeAndCreatedAtBetweenAndAmountBetweenOrderByCreatedAtDesc(String accountNumber,
                                                                                                                     TransactionType type,
                                                                                                                     LocalDateTime from,
                                                                                                                     LocalDateTime to,
                                                                                                                     BigDecimal minAmount,
                                                                                                                     BigDecimal maxAmount,
                                                                                                                     Pageable pageable
    );
}