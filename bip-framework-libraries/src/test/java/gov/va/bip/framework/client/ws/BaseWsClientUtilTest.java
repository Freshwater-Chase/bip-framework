package gov.va.bip.framework.client.ws;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import gov.va.bip.framework.client.ws.BaseWsClientUtil;

public class BaseWsClientUtilTest {

	private static final String TEST_FILE_LOCATION_WITHOUT_PREFIX = "test/file/location";
	private static final String TEST_FILE_LOCATION_WITH_FILE_PREFIX = "file:test/file/location";
	private static final String TEST_FILE_LOCATION_WITH_CLASSPATH_PREFIX = "classpath:test/file/location";

	@Test
	public void verifyAddFilePrefixWithoutPrefixTest() {
		String modifiedFilelocation = BaseWsClientUtil.verifyAddFilePrefix(TEST_FILE_LOCATION_WITHOUT_PREFIX);
		assertTrue(modifiedFilelocation.equals(TEST_FILE_LOCATION_WITH_FILE_PREFIX));
	}

	@Test
	public void verifyAddFilePrefixWithFilePrefixTest() {
		String modifiedFilelocation = BaseWsClientUtil.verifyAddFilePrefix(TEST_FILE_LOCATION_WITH_FILE_PREFIX);
		assertTrue(modifiedFilelocation.equals(TEST_FILE_LOCATION_WITH_FILE_PREFIX));
	}

	@Test
	public void verifyAddFilePrefixWithClassPathPrefixTest() {
		String modifiedFilelocation = BaseWsClientUtil.verifyAddFilePrefix(TEST_FILE_LOCATION_WITH_CLASSPATH_PREFIX);
		assertFalse(modifiedFilelocation.equals(TEST_FILE_LOCATION_WITH_FILE_PREFIX));
		assertTrue(modifiedFilelocation.equals(TEST_FILE_LOCATION_WITH_CLASSPATH_PREFIX));
	}

	@Test
	public void verifyAddFilePrefixWithEmptyStringTest() {
		String modifiedFilelocation = BaseWsClientUtil.verifyAddFilePrefix("");
		assertTrue(modifiedFilelocation.equals(""));
	}
}
