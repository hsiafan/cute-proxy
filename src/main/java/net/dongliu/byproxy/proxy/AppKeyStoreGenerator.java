package net.dongliu.byproxy.proxy;


import net.dongliu.byproxy.utils.NetUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.*;

import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_friendlyName;
import static org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_localKeyId;

/**
 * Dynamic generate self signed certificate for mitm proxy, with specified root ca private key and certificate.
 * JDK do not have an open api for this, although Open JDK/SUN JDK does have a internal api can work.
 * So we use  Bouncy Castle here.
 */
public class AppKeyStoreGenerator {
    private static Logger logger = LoggerFactory.getLogger(AppKeyStoreGenerator.class);
    private final X509Certificate caCertificate;
    private final RSAPrivateCrtKeyParameters privateKeyParameters;

    private final SecureRandom secureRandom;
    private final Random random;
    private final JcaX509ExtensionUtils jcaX509ExtensionUtils;

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }


    public AppKeyStoreGenerator(String caKeyStorePath, char[] caKeyStorePassword) throws Exception {

        logger.debug("Loading CA certificate/private key from file {}", caKeyStorePath);
        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = new FileInputStream(caKeyStorePath)) {
            caKeyStore.load(input, caKeyStorePassword);
        }

        Enumeration<String> aliases = caKeyStore.aliases();
        String alias = aliases.nextElement();
        logger.debug("Loading CA certificate/private by alias {}", alias);

        Key key = caKeyStore.getKey(alias, caKeyStorePassword);
        Objects.requireNonNull(key, "Specified key of the KeyStore not found!");
        RSAPrivateCrtKey privateCrtKey = (RSAPrivateCrtKey) key;
        privateKeyParameters = getPrivateKeyParameters(privateCrtKey);
        // and get the certificate

        caCertificate = (X509Certificate) caKeyStore.getCertificate(alias);
        Objects.requireNonNull(caCertificate, "Specified certificate of the KeyStore not found!");
        logger.debug("Successfully loaded CA key and certificate. CA DN is {}", caCertificate.getSubjectDN().getName());
        caCertificate.verify(caCertificate.getPublicKey());
        logger.debug("Successfully verified CA certificate with its own public key.");

        secureRandom = new SecureRandom();
        random = new Random();
        jcaX509ExtensionUtils = new JcaX509ExtensionUtils();
    }

    public BigInteger getCACertSerialNumber() {
        return caCertificate.getSerialNumber();
    }

    public byte[] exportCACertificate(boolean pem) throws CertificateEncodingException {
        byte[] data = caCertificate.getEncoded();
        if (!pem) {
            return data;
        }
        return ("-----BEGIN CERTIFICATE-----\n" +
                Base64.getMimeEncoder().encodeToString(data) +
                "\n-----END CERTIFICATE-----\n").getBytes(StandardCharsets.US_ASCII);
    }

    private RSAPrivateCrtKeyParameters getPrivateKeyParameters(RSAPrivateCrtKey privateCrtKey) {
        return new RSAPrivateCrtKeyParameters(privateCrtKey.getModulus(),
                privateCrtKey.getPublicExponent(),
                privateCrtKey.getPrivateExponent(),
                privateCrtKey.getPrimeP(), privateCrtKey.getPrimeQ(), privateCrtKey.getPrimeExponentP(),
                privateCrtKey.getPrimeExponentQ(),
                privateCrtKey.getCrtCoefficient());
    }

    public KeyStore generateKeyStore(String host, int validityDays, char[] password) throws Exception {
        logger.debug("Generating certificate for host {}", host);
        // generate the key pair for the new certificate
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, secureRandom);
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();

        Calendar calendar = Calendar.getInstance();
        // in case client time behind server time
        calendar.add(Calendar.DAY_OF_YEAR, -100);
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, validityDays + 100);
        Date expireDate = calendar.getTime();

        String appDName = "CN=ClearTheSky, OU=TianCao, O=TianCao, L=Beijing, ST=Beijing, C=CN";
        X500Name subject = new X500Name(appDName);
        ASN1ObjectIdentifier sigOID = PKCSObjectIdentifiers.sha256WithRSAEncryption;
        AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, DERNull.INSTANCE);

        V3TBSCertificateGenerator certificateGenerator = new V3TBSCertificateGenerator();
        certificateGenerator.setSerialNumber(new ASN1Integer(random.nextLong() + System.currentTimeMillis()));
        certificateGenerator.setIssuer(getSubject(caCertificate));
        certificateGenerator.setSubject(subject);
        certificateGenerator.setSignature(sigAlgId);
        certificateGenerator.setSubjectPublicKeyInfo(getPublicKeyInfo(publicKey));
        certificateGenerator.setStartDate(new Time(startDate));
        certificateGenerator.setEndDate(new Time(expireDate));

        // Set SubjectAlternativeName
        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
        extensionsGenerator.addExtension(Extension.subjectAlternativeName, false, () -> {
            ASN1EncodableVector nameVector = new ASN1EncodableVector();
            int hostType = NetUtils.getHostType(host);
            if (hostType == NetUtils.HOST_TYPE_IPV4 || hostType == NetUtils.HOST_TYPE_IPV6) {
                nameVector.add(new GeneralName(GeneralName.iPAddress, host));
            } else {
                nameVector.add(new GeneralName(GeneralName.dNSName, host));
            }
            return GeneralNames.getInstance(new DERSequence(nameVector)).toASN1Primitive();
        });
        Extensions x509Extensions = extensionsGenerator.generate();
        certificateGenerator.setExtensions(x509Extensions);

        TBSCertificate tbsCertificateStructure = certificateGenerator.generateTBSCertificate();
        byte[] data = toBinaryData(tbsCertificateStructure);
        byte[] signatureData = signData(sigOID, data, privateKeyParameters, secureRandom);

        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(tbsCertificateStructure);
        asn1EncodableVector.add(sigAlgId);
        asn1EncodableVector.add(new DERBitString(signatureData));

        DERSequence derSequence = new DERSequence(asn1EncodableVector);
        Certificate certificate = Certificate.getInstance(derSequence);
        X509CertificateObject clientCertificate = new X509CertificateObject(certificate);
        logger.debug("Verifying certificate for correct signature with CA public key");
        clientCertificate.verify(caCertificate.getPublicKey());
        clientCertificate.setBagAttribute(pkcs_9_at_friendlyName, new DERBMPString("Certificate for ByProxy App"));
        clientCertificate.setBagAttribute(pkcs_9_at_localKeyId,
                jcaX509ExtensionUtils.createSubjectKeyIdentifier(publicKey));
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);

        X509Certificate[] chain = new X509Certificate[]{clientCertificate, caCertificate};
        store.setKeyEntry("ByProxy app", privateKey, password, chain);
        return store;
    }


    private static byte[] signData(ASN1ObjectIdentifier sigOID, byte[] data,
                                   RSAPrivateCrtKeyParameters privateKeyParameters,
                                   SecureRandom secureRandom) throws Exception {
        PrivateKey caPrivateKey = KeyFactory.getInstance("RSA").generatePrivate(getKeySpec(privateKeyParameters));
        Signature signature = Signature.getInstance(sigOID.getId());
        signature.initSign(caPrivateKey, secureRandom);
        signature.update(data);
        return signature.sign();
    }

    private static byte[] toBinaryData(TBSCertificate tbsCertificateStructure) throws IOException {
        byte[] data;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            DEROutputStream derOutputStream = new DEROutputStream(bos);
            try {
                derOutputStream.writeObject(tbsCertificateStructure);
                data = bos.toByteArray();
            } finally {
                derOutputStream.close();
            }
        }
        return data;
    }

    private static X500Name getSubject(X509Certificate certificate) throws IOException, CertificateEncodingException {
        TBSCertificateStructure tbsCert = TBSCertificateStructure.getInstance(
                ASN1Primitive.fromByteArray(certificate.getTBSCertificate()));
        return tbsCert.getSubject();
    }

    private static SubjectPublicKeyInfo getPublicKeyInfo(PublicKey publicKey) throws IOException {
        try (InputStream bis = new ByteArrayInputStream(publicKey.getEncoded());
             ASN1InputStream asn1InputStream = new ASN1InputStream(bis)) {
            return SubjectPublicKeyInfo.getInstance(asn1InputStream.readObject());
        }
    }

    private static RSAPrivateCrtKeySpec getKeySpec(RSAPrivateCrtKeyParameters privateKeyParameters) {
        return new RSAPrivateCrtKeySpec(privateKeyParameters.getModulus(),
                privateKeyParameters.getPublicExponent(), privateKeyParameters.getExponent(),
                privateKeyParameters.getP(), privateKeyParameters.getQ(),
                privateKeyParameters.getDP(), privateKeyParameters.getDQ(), privateKeyParameters.getQInv());
    }


}
