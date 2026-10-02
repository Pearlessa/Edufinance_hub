/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.cput.eduhub.model;
import java.math.BigDecimal;
import java.sql.Timestamp;


/**
 *
 * @author Comaro Crossing
 */

public class Wallet {
    private int walletId;
    private int userId;
    private BigDecimal availableBalance;
    private Timestamp updatedAt;

    public Wallet() {}

    public Wallet(int walletId, int userId, BigDecimal availableBalance) {
        this.walletId = walletId;
        this.userId = userId;
        this.availableBalance = availableBalance;
    }

    public int getWalletId() { return walletId; }
    public void setWalletId(int walletId) { this.walletId = walletId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
