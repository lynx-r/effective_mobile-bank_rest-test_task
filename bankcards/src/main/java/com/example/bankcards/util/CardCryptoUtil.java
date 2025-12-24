package com.example.bankcards.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.example.bankcards.config.CryptoConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CardCryptoUtil {
  private static final String ALGORITHM = "AES";
  private static final int CARD_LENGTH = 16;

  private final CryptoConfig cryptoConfig;

  public String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < CardCryptoUtil.CARD_LENGTH) {
      return "**** **** **** ****";
    }
    return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
  }

  public String encrypt(String data) {
    try {
      String key = cryptoConfig.getEncryptionKey();
      SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] encryptedBytes = cipher.doFinal(data.getBytes());
      return Base64.getEncoder().encodeToString(encryptedBytes);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при шифровании карты", e);
    }
  }

  /**
   * Расшифровка номера карты
   */
  public String decrypt(String encryptedData) {
    try {
      String key = cryptoConfig.getEncryptionKey();
      SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
      return new String(cipher.doFinal(decodedBytes));
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при расшифровке карты", e);
    }
  }

}
