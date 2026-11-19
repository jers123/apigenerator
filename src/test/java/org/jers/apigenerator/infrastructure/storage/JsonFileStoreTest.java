package org.jers.apigenerator.infrastructure.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JsonFileStoreTest {

    record Sample(String name, boolean active) {
    }

    @TempDir
    private Path dataDir;

    private JsonFileStore<Sample> store() {
        return new JsonFileStoreFactory(JsonMapper.builder().build(), dataDir).create("sample.json", Sample.class);
    }

    private static ByteArrayResource defaults(final String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8), "valores-de-fabrica");
    }

    @Test
    void firstLoadCopiesTheDefaultsAndReadsOmittedBooleansAsFalse() {
        Sample loaded = store().loadOrInitialize(defaults("{\"name\": \"fabrica\"}"));

        assertThat(loaded).isEqualTo(new Sample("fabrica", false));
        assertThat(dataDir.resolve("sample.json")).exists();
    }

    @Test
    void anExistingFileIsNotOverwrittenByTheDefaults() throws IOException {
        Files.writeString(dataDir.resolve("sample.json"), "{\"name\": \"editado\", \"active\": true}");

        assertThat(store().loadOrInitialize(defaults("{\"name\": \"fabrica\"}")))
                .isEqualTo(new Sample("editado", true));
    }

    @Test
    void writeReplacesTheContentIndentedAndLeavesNoTemporaryFile() throws IOException {
        JsonFileStore<Sample> store = store();
        store.loadOrInitialize(defaults("{\"name\": \"fabrica\"}"));

        store.write(new Sample("nuevo", true));

        assertThat(store().loadOrInitialize(defaults("{}"))).isEqualTo(new Sample("nuevo", true));
        assertThat(Files.readString(dataDir.resolve("sample.json"))).contains("\n");
        assertThat(dataDir.resolve("sample.json.tmp")).doesNotExist();
    }

    @Test
    void aFailedWriteKeepsNoTemporaryFile() throws IOException {
        Path blocker = Files.createDirectories(dataDir.resolve("sample.json"));
        Files.writeString(blocker.resolve("ocupado.txt"), "x");

        assertThatIllegalStateException()
                .isThrownBy(() -> store().write(new Sample("nuevo", true)))
                .withMessageContaining("sample.json");
        assertThat(dataDir.resolve("sample.json.tmp")).doesNotExist();
    }

    @Test
    void missingDefaultsFailNamingTheResource() {
        assertThatIllegalStateException()
                .isThrownBy(() -> store().loadOrInitialize(new ClassPathResource("no-existe.json")))
                .withMessageContaining("no-existe.json");
        assertThat(dataDir.resolve("sample.json")).doesNotExist();
    }

    @Test
    void aCorruptFileFailsNamingTheFile() throws IOException {
        Files.writeString(dataDir.resolve("sample.json"), "{ esto no es json");

        assertThatIllegalStateException()
                .isThrownBy(() -> store().loadOrInitialize(defaults("{}")))
                .withMessageContaining("sample.json");
    }
}