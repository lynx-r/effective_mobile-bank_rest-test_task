package com.example.bankcards.util;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CardGenerator {
  private static final Random RANDOM = new Random();

  /**
   * Генерирует 16-значный номер карты, валидный по алгоритму Луна.
   * 
   * @param bin Bank Identification Number (первые 6 цифр). Например, "444455"
   */
  public static String generate(String bin) {
    // 1. Берем BIN (6 цифр) и добавляем 9 случайных цифр
    String partialNumber = bin + IntStream.range(0, 9)
        .mapToObj(i -> String.valueOf(RANDOM.nextInt(10)))
        .collect(Collectors.joining());

    // 2. Вычисляем 16-ю контрольную цифру по алгоритму Луна
    int checkDigit = CardGenerator.calculateLuhnCheckDigit(partialNumber);

    return partialNumber + checkDigit;
  }

  private static int calculateLuhnCheckDigit(String number) {
    int sum = 0;
    boolean alternate = true;

    // Идем с конца строки (справа налево)
    for (int i = number.length() - 1; i >= 0; i--) {
      int n = Integer.parseInt(number.substring(i, i + 1));
      if (alternate) {
        n *= 2;
        if (n > 9) {
          n -= 9;
        }
      }
      sum += n;
      alternate = !alternate;
    }

    return (10 - (sum % 10)) % 10;
  }

}
