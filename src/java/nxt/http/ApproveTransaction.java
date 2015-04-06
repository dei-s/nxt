package nxt.http;


import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.PhasingPoll;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static nxt.http.JSONResponses.MISSING_TRANSACTION_FULL_HASH;
import static nxt.http.JSONResponses.PHASING_TRANSACTION_FINISHED;
import static nxt.http.JSONResponses.TOO_MANY_PHASING_VOTES;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION_FULL_HASH;

public class ApproveTransaction extends CreateTransaction {
    static final ApproveTransaction instance = new ApproveTransaction();

    private ApproveTransaction() {
        super(new APITag[]{APITag.CREATE_TRANSACTION, APITag.PHASING}, "transactionFullHash", "transactionFullHash", "transactionFullHash",
                "revealedSecret", "revealedSecretText");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        String[] phasedTransactionValues = req.getParameterValues("transactionFullHash");

        if (phasedTransactionValues.length == 0) {
            return MISSING_TRANSACTION_FULL_HASH;
        }

        if (phasedTransactionValues.length > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
            return TOO_MANY_PHASING_VOTES;
        }

        List<byte[]> phasedTransactionFullHashes = new ArrayList<>(phasedTransactionValues.length);
        for (String phasedTransactionValue : phasedTransactionValues) {
            byte[] hash = Convert.parseHexString(phasedTransactionValue);
            PhasingPoll phasingPoll = PhasingPoll.getPoll(Convert.fullHashToId(hash));
            if (phasingPoll == null) {
                return UNKNOWN_TRANSACTION_FULL_HASH;
            }
            phasedTransactionFullHashes.add(hash);
        }

        byte[] secret = Convert.parseHexString(Convert.emptyToNull(req.getParameter("revealedSecret")));
        if (secret == null) {
            String secretText = Convert.emptyToNull(req.getParameter("revealedSecretText"));
            if (secretText != null) {
                secret = Convert.toBytes(secretText);
            }
        }
        Account account = ParameterParser.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingPhasingVoteCasting(phasedTransactionFullHashes, secret);
        return createTransaction(req, account, attachment);
    }
}
