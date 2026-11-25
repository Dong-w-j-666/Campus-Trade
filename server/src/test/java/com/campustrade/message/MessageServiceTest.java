package com.campustrade.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageServiceTest {

    @Test
    void masksMainlandMobileNumbersWithoutChangingOtherDigits() {
        String content = "联系电话 13800138000，订单号 2026112512345678。";

        assertThat(MessageService.maskPhoneNumbers(content))
                .isEqualTo("联系电话 138****8000，订单号 2026112512345678。");
    }

    @Test
    void masksEveryStandaloneMobileNumber() {
        String content = "13800138000 / 19912345678";

        assertThat(MessageService.maskPhoneNumbers(content))
                .isEqualTo("138****8000 / 199****5678");
    }

    @Test
    void normalizesWhitespaceAndLineEndings() {
        assertThat(MessageService.normalizeContent(" \r\n 明天下午见 \r\n"))
                .isEqualTo("明天下午见");
        assertThat(MessageService.normalizeContent(null)).isEmpty();
    }
}
