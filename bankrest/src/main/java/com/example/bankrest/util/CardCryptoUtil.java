package com.example.bankrest.util;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class CardCryptoUtil {
  // В реальном проекте ключ должен быть в Vault или переменной окружения!
  private static final String ALGORITHM = "AES";
  // TODO: брать из конфига
  private static final String KEY = "12345678123456781234567812345678"; // 32 байта для AES-256
  private static final int CARD_LENGTH = 12;

  /**
   * Маскирование номера карты (оставляет первые 4 и последние 4 цифры)
   */
  public String maskCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.length() < CardCryptoUtil.CARD_LENGTH) {
      return "**** **** **** ****";
    }
    return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
  }

  /**
   * Шифрование номера карты (AES-256)
   */
  public String encrypt(String data) {
    try {
      SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
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
      SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
      return new String(cipher.doFinal(decodedBytes));
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при расшифровке карты", e);
    }
  }

}
