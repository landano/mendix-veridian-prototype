package cardanowallet;

import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;

import java.util.ArrayList;
import java.util.List;

public class MetadataUtils {

	public static List<String> splitMetadata(String longString, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        StringBuilder builder = new StringBuilder(longString);
        while (builder.length() > 0) {
            chunks.add(builder.substring(0, Math.min(chunkSize, builder.length())));
            builder.delete(0, Math.min(chunkSize, builder.length()));
        }
        return chunks;
    }
    
    public static MessageMetadata createMessageMetadata(String longMetadata) {
    	// Split the long metadata into chunks of 64 characters
    	List<String> metadataChunks = MetadataUtils.splitMetadata(longMetadata, 60);
    	
    	// Create metadata and add chunks
    	MessageMetadata metadata = MessageMetadata.create();
    	for (int i = 0; i < metadataChunks.size(); i++) {
    	    metadata.add(metadataChunks.get(i));
    	}
    	
    	return metadata;
    }
    
	public static ILogNode LOG = Core.getLogger(cardanowallet.proxies.constants.Constants.getLogNodeName());

}