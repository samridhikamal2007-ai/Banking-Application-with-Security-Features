package com.bank.service;

import com.bank.model.Account;
import com.bank.model.AuditLog;
import com.bank.model.Transaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuditLogRepository;
import com.bank.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AccountService {
    private final AccountRepository accountRepo = new AccountRepository();
    private final TransactionRepository transactionRepo = new TransactionRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    public Account createAccount(int userId, String accountType) {
        String accNum = accountRepo.generateUniqueAccountNumber();
        Account account = new Account();
        account.setAccountNumber(accNum);
        account.setUserId(userId);
        account.setAccountType(accountType.toUpperCase());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus("ACTIVE");

        boolean saved = accountRepo.save(account);
        if (saved) {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("ACCOUNT_CREATION");
            log.setDescription("Created " + accountType + " account: " + accNum);
            auditRepo.save(log);
            return account;
        }
        return null;
    }

    public List<Account> getAccountsByUserId(int userId) {
        return accountRepo.findByUserId(userId);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepo.findByAccountNumber(accountNumber);
    }

    public boolean deposit(String accountNumber, BigDecimal amount, int userId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        Account account = accountRepo.findByAccountNumber(accountNumber);
        if (account == null || !"ACTIVE".equals(account.getStatus())) return false;

        account.setBalance(account.getBalance().add(amount));
        boolean updated = accountRepo.update(account);
        
        if (updated) {
            String txId = UUID.randomUUID().toString();
            Transaction tx = new Transaction();
            tx.setTransactionId(txId);
            tx.setFromAccount(null);
            tx.setToAccount(accountNumber);
            tx.setTransactionType("DEPOSIT");
            tx.setAmount(amount);
            tx.setDescription("Deposit of $" + amount + " to " + accountNumber);
            transactionRepo.save(tx);

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("DEPOSIT");
            log.setDescription("Deposited $" + amount + " to account " + accountNumber + ". Transaction ID: " + txId);
            auditRepo.save(log);
        }
        return updated;
    }

    public boolean withdraw(String accountNumber, BigDecimal amount, int userId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        Account account = accountRepo.findByAccountNumber(accountNumber);
        if (account == null || !"ACTIVE".equals(account.getStatus())) return false;

        if (account.getBalance().compareTo(amount) < 0) return false;

        account.setBalance(account.getBalance().subtract(amount));
        boolean updated = accountRepo.update(account);

        if (updated) {
            String txId = UUID.randomUUID().toString();
            Transaction tx = new Transaction();
            tx.setTransactionId(txId);
            tx.setFromAccount(accountNumber);
            tx.setToAccount(null);
            tx.setTransactionType("WITHDRAWAL");
            tx.setAmount(amount);
            tx.setDescription("Withdrawal of $" + amount + " from " + accountNumber);
            transactionRepo.save(tx);

            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction("WITHDRAWAL");
            log.setDescription("Withdrew $" + amount + " from account " + accountNumber + ". Transaction ID: " + txId);
            auditRepo.save(log);
        }
        return updated;
    }
}
