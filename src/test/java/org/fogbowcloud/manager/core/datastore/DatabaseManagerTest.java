package org.fogbowcloud.manager.core.datastore;

import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.models.linkedlists.SynchronizedDoublyLinkedList;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.lang.reflect.Field;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesHolder.class)
public class DatabaseManagerTest {

    private static final String DATABASE_PATH = "/tmp/fogbow_history_test.db";
    private String DATABASE_URL = "jdbc:sqlite:" + DATABASE_PATH;

    /**
     * Cleaning all the enviromnent before running the tests.
     */
    @BeforeClass
    public static void init() throws NoSuchFieldException, IllegalAccessException {
        resetDatabaseManagerInstance();
        cleanEnviromnent();
    }

    @Before
    public void setUp() {
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(Mockito.anyString())).thenReturn(DATABASE_URL);

        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }

    /**
     * Remove database file and reset the database manager.
     */
    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        resetDatabaseManagerInstance();
        cleanEnviromnent();
    }

    // test case: Trying to initialize the datastore when an invalid database URL is passed.
    @Test(expected = FatalErrorException.class)
    public void testInitializeDataStoreWithError() {
        // set up
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperty(Mockito.anyString())).thenReturn("invalid_url");

        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        // exercise: it should raise an exception since the database path is an invalid one
        DatabaseManager.getInstance();
    }

    /**
     * Returns size of synchronizedDoublyLinkedList.
     */
    protected int getListSize(SynchronizedDoublyLinkedList synchronizedDoublyLinkedList) {
        int listSize = 0;

        while (synchronizedDoublyLinkedList.getNext() != null) {
            listSize++;
        }

        return listSize;
    }

    private static void resetDatabaseManagerInstance() throws NoSuchFieldException, IllegalAccessException {
        Field instance = DatabaseManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static void cleanEnviromnent() {
        File folder = new File(DATABASE_PATH);
        File[] listFiles = folder.listFiles();

        if (listFiles != null) {
            for (File file : listFiles) {
                if (file != null) {
                    file.delete();
                    file.deleteOnExit();
                }
            }
        }

        folder.delete();
        folder.deleteOnExit();
    }
}
