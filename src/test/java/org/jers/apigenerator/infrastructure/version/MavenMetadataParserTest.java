package org.jers.apigenerator.infrastructure.version;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MavenMetadataParserTest {

    @Test
    void readsTheVersionsList() {
        String xml = """
                <metadata>
                  <groupId>com.h2database</groupId>
                  <artifactId>h2</artifactId>
                  <versioning>
                    <latest>2.4.240</latest>
                    <versions><version>2.3.232</version><version>2.4.240</version></versions>
                  </versioning>
                </metadata>
                """;

        assertThat(MavenMetadataParser.versions(xml.getBytes(StandardCharsets.UTF_8)))
                .containsExactly("2.3.232", "2.4.240");
    }

    @Test
    void rejectsDoctypeToPreventXxe() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE metadata [<!ENTITY xxe SYSTEM "file:///C:/Windows/win.ini">]>
                <metadata><versioning><versions><version>&xxe;</version></versions></versioning></metadata>
                """;

        assertThatIllegalArgumentException()
                .isThrownBy(() -> MavenMetadataParser.versions(xml.getBytes(StandardCharsets.UTF_8)));
    }
}