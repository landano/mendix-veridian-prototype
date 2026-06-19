package cardanowallet;

import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionWitnessSet;
import com.bloxbean.cardano.client.transaction.spec.VkeyWitness;
import java.util.ArrayList;
import java.util.List;

public class TransactionHandler {

    public void addWitness(Transaction transaction, Transaction witnessSignedTxn) {
        TransactionWitnessSet witnessSet = transaction.getWitnessSet();
        List<VkeyWitness> vkeyWitnesses = witnessSet.getVkeyWitnesses();

        // Check if vkeyWitnesses is null and initialize if necessary
        if (vkeyWitnesses == null) {
            vkeyWitnesses = new ArrayList<>();
            witnessSet.setVkeyWitnesses(vkeyWitnesses);
        }

        // Add the witness
        vkeyWitnesses.add(witnessSignedTxn.getWitnessSet().getVkeyWitnesses().get(0));
    }
}