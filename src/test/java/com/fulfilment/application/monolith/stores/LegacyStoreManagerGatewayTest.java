package com.fulfilment.application.monolith.stores;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class LegacyStoreManagerGatewayTest {

    @Inject
    LegacyStoreManagerGateway gateway;

    @Test
    void testCreateStoreOnLegacySystem_completesWithoutException() {
        Store store = storeOf("CreateTest", 10);

        assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    }

    @Test
    void testCreateStoreOnLegacySystem_withZeroStock_completesWithoutException() {
        Store store = storeOf("EmptyStock", 0);

        assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    }

    @Test
    void testCreateStoreOnLegacySystem_withLargeStock_completesWithoutException() {
        Store store = storeOf("BigStore", Integer.MAX_VALUE);

        assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    }

    @Test
    void testCreateStoreOnLegacySystem_withSpecialCharactersInName_completesWithoutException() {
        // Store names with spaces and symbols should still produce a valid temp file prefix
        Store store = storeOf("Store & Co.", 5);

        assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    }

    @Test
    void testUpdateStoreOnLegacySystem_completesWithoutException() {
        Store store = storeOf("UpdateTest", 20);

        assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
    }

    @Test
    void testUpdateStoreOnLegacySystem_withZeroStock_completesWithoutException() {
        Store store = storeOf("UpdatedEmptyStock", 0);

        assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
    }

    @Test
    void testUpdateStoreOnLegacySystem_withSpecialCharactersInName_completesWithoutException() {
        Store store = storeOf("Updated & Store #2", 15);

        assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
    }

    @Test
    void testCreateStore_leavesNoTempFileBehind(@TempDir Path tempDir) throws IOException {
        // Count .txt files in the system temp dir before and after to confirm cleanup
        Path systemTmp = Path.of(System.getProperty("java.io.tmpdir"));
        long before = countTxtFiles(systemTmp);

        Store store = storeOf("LeakCheck", 3);
        gateway.createStoreOnLegacySystem(store);

        long after = countTxtFiles(systemTmp);
        assertEquals(before, after, "Expected no temp .txt files to remain after createStore");
    }

    @Test
    void testUpdateStore_leavesNoTempFileBehind(@TempDir Path tempDir) throws IOException {
        Path systemTmp = Path.of(System.getProperty("java.io.tmpdir"));
        long before = countTxtFiles(systemTmp);

        Store store = storeOf("UpdateLeakCheck", 7);
        gateway.updateStoreOnLegacySystem(store);

        long after = countTxtFiles(systemTmp);
        assertEquals(before, after, "Expected no temp .txt files to remain after updateStore");
    }

    @Test
    void testCreateStore_withNullName_doesNotPropagateException() {
        // writeToFile swallows exceptions via e.printStackTrace().
        // We verify the gateway honours that contract and never throws.
        Store store = new Store();
        store.name = null;
        store.quantityProductsInStock = 0;

        // Files.createTempFile will throw with a null prefix; the catch block
        // inside writeToFile should absorb it silently.
        assertDoesNotThrow(() -> gateway.createStoreOnLegacySystem(store));
    }

    @Test
    void testUpdateStore_withNullName_doesNotPropagateException() {
        Store store = new Store();
        store.name = null;
        store.quantityProductsInStock = 0;

        assertDoesNotThrow(() -> gateway.updateStoreOnLegacySystem(store));
    }

    private static Store storeOf(String name, int qty) {
        Store s = new Store();
        s.name = name;
        s.quantityProductsInStock = qty;
        return s;
    }

    private static long countTxtFiles(Path dir) throws IOException {
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(".txt"))
                    .count();
        }
    }
}
