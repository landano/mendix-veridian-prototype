package cardanowallet;

import com.bloxbean.cardano.client.spec.Script;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class TransactionOutputDeserializer extends JsonDeserializer<TransactionOutput> {
    @Override
    public TransactionOutput deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        TransactionOutput output = new TransactionOutput();

        // Deserialize fields manually
        if (node.has("scriptRef")) {
            JsonNode scriptRefNode = node.get("scriptRef");
            // Handle scriptRef deserialization based on its type
            if (scriptRefNode.isBinary()) {
                output.setScriptRef(scriptRefNode.binaryValue());
            } else {
                // Deserialize as Script object
                Script scriptRef = p.getCodec().treeToValue(scriptRefNode, Script.class);
                output.setScriptRef(scriptRef);
            }
        }

        // Deserialize other fields
        // ...

        return output;
    }
}
