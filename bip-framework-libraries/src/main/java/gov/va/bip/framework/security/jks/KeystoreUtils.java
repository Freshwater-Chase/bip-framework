package gov.va.bip.framework.security.jks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;


public class KeystoreUtils {

    private static final String PEM_CERTIFICATE_PREFIX = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_CERTIFICATE_SUFFIX = "-----END CERTIFICATE-----";
    private static final String PRIVATE_KEY_PREFIX = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_SUFFIX = "-----END PRIVATE KEY-----";

    
    /**
	 * Private constructor to prevent instantiation.
	 */
	private KeystoreUtils() {
    }
    
	
    /**
     * Create a KeyStore from the given private/public key pair.
     * @param publicCert provided in PEM format
     * @param privateKey provided in PEM format
     * @param privateKeyPassword used to protect the given private key
     * @param alias used to name the entry in the KeyStore
     * @return KeyStore object containing the give private/public key pair stored under the given alias
     * @throws KeyStoreException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws InvalidKeySpecException
     */
    public static KeyStore createClientStore(String publicCert, String privateKey, String privateKeyPassword, String alias) throws 
    			KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, InvalidKeySpecException {
        //Create Certificate from PEM format string
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        Certificate publicX509 = parseCertificateString(publicCert, certFactory);
        Certificate[] certChain = new Certificate[1];
        certChain[0] = publicX509;

        //Create PrivateKey from PEM format string
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateX509 = parsePrivateKeyString(privateKey, keyFactory);


        KeyStore clientStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientStore.load(null, null);
        if (privateKeyPassword != null) {
            clientStore.setKeyEntry(alias, privateX509, privateKeyPassword.toCharArray(), certChain);
        } else {
            clientStore.setKeyEntry(alias, privateX509, "".toCharArray(), certChain);
        }
        
        return clientStore;

    }

    /**
     * Convert a PEM format private key into a PrivateKey object
     * @param privateKey in PEM format
     * @param keyFactory defining the format of the PrivateKey to generate
     * @return  PrivateKey object
     * @throws InvalidKeySpecException
     */
    protected static PrivateKey parsePrivateKeyString(String privateKey, KeyFactory keyFactory) throws InvalidKeySpecException {
    		// strip off PEM markers, if any, so the certificate is in Base64-encoded DER format
        // (possibly with extraneous whitespace)
        String base64DER = privateKey.replaceAll(PRIVATE_KEY_PREFIX + "|" + PRIVATE_KEY_SUFFIX, "");
        byte[] decodedKey = Base64.getMimeDecoder().decode(base64DER);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        return keyFactory.generatePrivate(keySpec);
        
    }

    
    /**
     * Convert a PEM format certificate into a Certificate object
     * @param certificateString in PEM format
     * @param certFactory defining the format of the Certificate to generate
     * @return Certificate object
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     */
    private static Certificate parseCertificateString(String certificateString, CertificateFactory certFactory) 
            throws CertificateException, KeyStoreException, IOException{
    	
    		// strip off PEM markers, if any, so the certificate is in Base64-encoded DER format
        // (possibly with extraneous whitespace)
        String base64DER = certificateString.replaceAll(PEM_CERTIFICATE_PREFIX + "|" + PEM_CERTIFICATE_SUFFIX, "");
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getMimeDecoder().decode(base64DER));) {
        		return certFactory.generateCertificate(bis);
        }
    }
}