package bisq.core.dao.voting;

import bisq.core.dao.state.blockchain.Tx;

import org.bitcoinj.core.Coin;

import lombok.Getter;

import javax.annotation.Nullable;

@Getter
public class ValidationException extends Exception {
    @Nullable
    private Coin requestedBsq;
    @Nullable
    private Coin minRequestAmount;
    @Nullable
    private Tx tx;

    public ValidationException(String message, Coin requestedBsq, Coin minRequestAmount) {
        super(message);
        this.requestedBsq = requestedBsq;
        this.minRequestAmount = minRequestAmount;
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public ValidationException(String message, Tx tx) {
        super(message);
        this.tx = tx;
    }

    public ValidationException(Throwable cause, Tx tx) {
        super(cause);
        this.tx = tx;
    }
}