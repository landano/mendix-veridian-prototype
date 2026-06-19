package cardanowallet;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Network;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.util.HexUtil;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;


public class Utils extends BaseUtils {
	private Network selectedNetwork;
	private String blockfrostUrl;
	
	public Utils(String network) {
        String bfProjectId = cardanowallet.proxies.constants.Constants.getBLOCKFROST_PROJECTID();
        
        // Change backendService based on network parameter
        switch(network.toLowerCase()) {
            case "preprod":
                backendService = new BFBackendService(Constants.BLOCKFROST_PREPROD_URL, bfProjectId);
                break;
            case "preview":
                backendService = new BFBackendService(Constants.BLOCKFROST_PREVIEW_URL, bfProjectId);
                break;
            case "mainnet":
                backendService = new BFBackendService(Constants.BLOCKFROST_MAINNET_URL, bfProjectId);
                break;
            default:
                throw new IllegalArgumentException("Invalid network: " + network);
        }
        assignNetwork(network);
        // Reinitialize services with new backendService
        initializeServices();
    }
	
	public static Integer NetworkEnumToInt(cardanowallet.proxies.Enum_CardanoNetwork network) {
		switch (network) {
		case Mainnet:
			return 1;
		case Preprod:
			return 0;
		case Preview:
			return 0;
		default:
			return 1;
		}
	}
	
	private void assignNetwork(String networkString) {
		networkString = networkString == null? "": networkString;
		if(networkString.equalsIgnoreCase("preprod")) {
			this.selectedNetwork = Networks.preprod();
			this.blockfrostUrl = Constants.BLOCKFROST_PREPROD_URL;
		} else if(networkString.equalsIgnoreCase("testnet")) {
			this.selectedNetwork = Networks.testnet();
			this.blockfrostUrl = Constants.BLOCKFROST_TESTNET_URL;
		} else if(networkString.equalsIgnoreCase("preview")) {
			this.blockfrostUrl = Constants.BLOCKFROST_PREVIEW_URL;
			this.selectedNetwork = Networks.preview();
		} else {
			this.selectedNetwork = Networks.mainnet();
			this.blockfrostUrl = Constants.BLOCKFROST_MAINNET_URL;
		}
	}
	public Network getCardanoNetwork() {
		return this.selectedNetwork;
	}
	
	
	public String getBlockfrostUrl() {
		return this.blockfrostUrl;
	}
	public void setCardanoNetwork(String selectedCardanoNetwork) {
		this.assignNetwork(selectedCardanoNetwork);
	}
    
    private void initializeServices() {
        feeCalculationService = backendService.getFeeCalculationService();
        transactionHelperService = backendService.getTransactionHelperService();
        transactionService = backendService.getTransactionService();
        blockService = backendService.getBlockService();
        assetService = backendService.getAssetService();
        utxoService = backendService.getUtxoService();
        networkInfoService = backendService.getNetworkInfoService();
        epochService = backendService.getEpochService();
        utxoTransactionBuilder = backendService.getUtxoTransactionBuilder();
        utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());
        protocolParamsSupplier = new DefaultProtocolParamsSupplier(epochService);
    }
    
    // Optional: Method to change network after initialization
    public void changeNetwork(String network) {
        String bfProjectId = cardanowallet.proxies.constants.Constants.getBLOCKFROST_PROJECTID();
        
        switch(network.toLowerCase()) {
            case "preprod":
                backendService = new BFBackendService(Constants.BLOCKFROST_PREPROD_URL, bfProjectId);
                break;
            case "preview":
                backendService = new BFBackendService(Constants.BLOCKFROST_PREVIEW_URL, bfProjectId);
                break;
            case "mainnet":
                backendService = new BFBackendService(Constants.BLOCKFROST_MAINNET_URL, bfProjectId);
                break;
            default:
                throw new IllegalArgumentException("Invalid network: " + network);
        }
        
        initializeServices();
    }
	
	public static String ConvertPublicAddressToHex(String addressToHex) throws Exception {
		String pubKeyHashHex = "";
        try {
            // The test address
//            String addressStr = "addr_test1qr25tw2qpyfvpnmdxrc4ythrl3zq8dpk8ggwxxd230xndquvjw44r906qaszsqktacayq37zlavvft875g8afrd6mazsgjc6gz";
        	if(addressToHex == null || addressToHex.isEmpty()) {
        		var err = "Cannot converAddress is empty";
        		System.out.println(err);
        	}
           String addressStr = addressToHex; //!=null ?addressToHex : addressStr;
            // Create Address instance
            Address address = new Address(addressToHex);

            // Get the payment credential hash (public key hash)
            byte[] pubKeyHash = address.getPaymentCredentialHash()
                    .orElseThrow(() -> new RuntimeException("No payment credential found in address"));

            // Convert byte array to hex string for display
            pubKeyHashHex = HexUtil.encodeHexString(pubKeyHash);

            LOG.debug("Address: " + addressToHex);
            LOG.debug("Public Key Hash: " + pubKeyHashHex);

            // Verify if it's a pubkey hash (not a script hash)
            if (address.isPubKeyHashInPaymentPart()) {
            	LOG.debug("Confirmed: This is a public key hash");
            } else {
            	LOG.warn("Warning: This is not a public key hash");
            }
            

        } catch (Exception e) {
        	throw new Exception("Error processing address: " + e.getMessage());
        }
        return pubKeyHashHex;
    }
	
	public static ILogNode LOG = Core.getLogger(cardanowallet.proxies.constants.Constants.getLogNodeName());

}
