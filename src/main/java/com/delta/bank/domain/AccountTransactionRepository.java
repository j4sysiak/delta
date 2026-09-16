package com.delta.bank.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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

    java.util.Optional<AccountTransactionEntity> findFirstByTransferRequestIdAndAccountNumberAndType(
            String transferRequestId,
            String accountNumber,
            TransactionType type
    );
}