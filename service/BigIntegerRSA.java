package weibo.service;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:24
 * To change this template use File | Settings | File Templates.
 */
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BigIntegerRSA {
    private static final Log logger = LogFactory.getLog(BigIntegerRSA.class);
    /*public static String SINA_PUB = "EB2A38568661887FA180BDDB5CABD5F21C7BFD59C090CB2D24"
            + "5A87AC253062882729293E5506350508E7F9AA3BB77F4333231490F915F6D63C55FE2F08A49B353F444AD39"
            + "93CACC02DB784ABBB8E42A9B1BBFFFB38BE18D78E87A0E41B9B8F73A928EE0CCEE"
            + "1F6739884B9777E4FE9E88A1BBE495927AC4A799B3181D6442443";*/

    public String rsaCrypt(String modeHex, String exponentHex, String messageg){

        BigInteger m = new BigInteger(modeHex, 16); /* public exponent */
        BigInteger e = new BigInteger(exponentHex, 16); /* modulus */
        RSAPublicKeySpec spec = new RSAPublicKeySpec(m, e);

        RSAPublicKey pub;
        byte[] encryptedContentKey = null;
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            pub = (RSAPublicKey) factory.generatePublic(spec);
            Cipher enc = Cipher.getInstance("RSA");
            enc.init(Cipher.ENCRYPT_MODE, pub);
            encryptedContentKey = enc.doFinal(messageg.getBytes("GB2312"));
        } catch (InvalidKeySpecException e1) {
            logger.error( e1);
        } catch (NoSuchAlgorithmException e1) {
            logger.error( e1);
        } catch (NoSuchPaddingException e1) {
            logger.error( e1);
        } catch (InvalidKeyException e1) {
            logger.error( e1);
        } catch (IllegalBlockSizeException e1) {
            logger.error( e1);
        } catch (BadPaddingException e1) {
            logger.error( e1);
        } catch (UnsupportedEncodingException e1) {
            logger.error( e1);
        }

        return new String(Hex.encodeHex(encryptedContentKey));
    }
}
