package com.delta.bank.lab.java21.switchpatterns;

public sealed interface BankEvent
        // permits w Javie określa, jakie klasy lub interfejsy mogą implementować albo rozszerzać typ sealed.
        // oznacza to, że tylko te trzy typy mogą być BankEvent:
        //  - DepositRecorded
        //  - WithdrawalRecorded
        //  - TransferRecorded

        // Każda z tych klas musi też jawnie zadeklarować jeden z modyfikatorów:
        //  final – nie da się już po niej dziedziczyć,
        //  sealed – dalej ogranicza potomków,
        //  non-sealed – zdejmuje ograniczenie i pozwala dziedziczyć swobodnie.

        // To jest używane głównie po to, żeby zamknąć hierarchię typów
        //   — np. bardzo przydatne przy switch pattern matching, bo kompilator wie, jakie wszystkie warianty są możliwe.

        // Jeśli te typy są rekordami (record), nie muszą mieć jawnie
        // final/sealed/non-sealed, bo record jest domyślnie final.
        permits DepositRecorded, WithdrawalRecorded, TransferRecorded {
}