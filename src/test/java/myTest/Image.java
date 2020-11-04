package myTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.plugins.cloudidp.googlecloud.GoogleCloudIdentityProviderPlugin;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.image.v1.GoogleCloudImagePlugin;

public class Image {

	@Test
	public void test() throws FogbowException {
		GoogleCloudIdentityProviderPlugin gp = new GoogleCloudIdentityProviderPlugin();
		Map<String, String> map = new HashMap<String, String>();
		map.put("project_id", "crested-bloom-286214");
		map.put("email", "analytics@crested-bloom-286214.iam.gserviceaccount.com");
		map.put("private_key", "-----BEGIN PRIVATE KEY-----MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDAaAh7Sur/X0zuAqwa3XC0FQyC5pCuFtBWmEnVh80K7PVKaI6MwV86xztKClY0tgIyW3jBMnCLPM/ONsCxaCC+bkeeQgHhYU++dwEdqIa0EtQnjtl/ggXMghXX1ZszOVIh5n9yp+fKFyZpWigbMrcOi/eAR7+wnCqXKjZ8eXivnH010hntHLsv/ZyVhC4pJotnLey/xDAdJlb/Ap6EgKjnGZpfMGL6OpYsec9RNPpUguT0bv5aqT+RpjGfSN97SblUGG47kjAw/RbnsuvCWjRXLwXHaYxdRgMq2HIP7+pSf7IQRr0ouAa7SUN9fr/9l1Ypito7cT1cLl5U2KBSsFFZAgMBAAECggEAE+txXVpfQ/sY2tdZ/BoK7gnz1qWKOwvPm/d+RWYe20+3AXIbuATZGBvCTqMNU724MoB32wCe2Dt+hCn0/yj5ot2F+qN40F2jxONLMUfeUkxUvJe0etf6Q7cADe1meQIpN6tGbJdnjuugpW2ZE1/HxfDFufkzplQ0sJvFMlPdI8kQxuSftuhnIYPly1+sq+BHA8ttL/zdURAKVJYdVduHeUbqI9mSE0wGom1opMikskj6DnMTWRyJsPOKQwA7CiTyXfn1OMdDC9+CiL2vG6Ds55dBKwSzfY8UPcftY+TuFyGPPRSt+s5pZL6lvSiRr2/6HXO3+3WuHASYJdb9od4mAQKBgQDpHYiB1FijtveH1WZGA55XJ75+9QykjqEIqhmGqXSl78/PwTQaXti9dS9+BzQEDnP3Z6ZywivBa3IQ13QQz/17yNEINzdB8UHer1cA8htmeL6VY6dCXQe30pWAAgaFEDg9LUq6hVPIu4ywhOIiEkNLRHYpErmSaOk1cIjXz0BnGQKBgQDTS29DhepPnlaCpgcKYos6BI8yzcVrHIYosOFS3etmbilnNOrkjR3IoDKex6KBRlWycr6XljsOo0VTNKB5Tx+MZzv1cJXL6MeeFRxY77YMq5xKbDcohj1ldZv7iAhVv9vV+vI8Yo5hHrYt788bG6xsfKZQhmocTa7frb+ZF3/EQQKBgCqbFTn1X4X8iN0wqZKeUrBHOU/m5bqlvtTgke3ExucVH3wvKaCwORjXdCiqlF0xbwyGyysqRekCBEUDu9jeyst29o1z2guZVpqmnVY06cEezGZtYkKE0kZMnLpapGppfn3f60qP17JWZO5WOyZTBC2bg6UaIQSbXBRCTyByZqjxAoGAckECIzMQpojqIq+Acx8iRfcdL82RCQBdkzdCQDr8BWFgRgyZT4j3J/toI++zcdAEmv/tC68StDGZVQrKE46zcoqII4oDlkWDW3ny4CyO9n79fkjR8rnUDT7xX6wJRcT/LNALmJd6gg3HWUOpa/Ek5WzpyfVE/5UKEK21QyUv04ECgYB21bPBUygudipE1SYniqIuJS8bb61VneU+RAfPpxLsgwSImzD6993GysHei4MiRvoUQAI9FcCTJGC2q1+bs6Gh+/RN4UjcskP+9NIIO5rMsBmqZivXlYVKd7TZkA/f51rw3vIjVQNEtSZQcUfjV6x2q3ixIdAPNsU8eSmKwsCQUw==-----END PRIVATE KEY-----");
		GoogleCloudUser gc = gp.getCloudUser(map);
		GoogleCloudImagePlugin gi = new GoogleCloudImagePlugin("src/main/resources/private/clouds/google-cloud/cloud.conf");
		gi.getImage("1725143366872340894", gc);
	}
	
	@Test
	public void test2() throws FogbowException {
		GoogleCloudIdentityProviderPlugin gp = new GoogleCloudIdentityProviderPlugin();
		Map<String, String> map = new HashMap<String, String>();
		map.put("project_id", "crested-bloom-286214");
		map.put("email", "analytics@crested-bloom-286214.iam.gserviceaccount.com");
		map.put("private_key", "-----BEGIN PRIVATE KEY-----MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDAaAh7Sur/X0zuAqwa3XC0FQyC5pCuFtBWmEnVh80K7PVKaI6MwV86xztKClY0tgIyW3jBMnCLPM/ONsCxaCC+bkeeQgHhYU++dwEdqIa0EtQnjtl/ggXMghXX1ZszOVIh5n9yp+fKFyZpWigbMrcOi/eAR7+wnCqXKjZ8eXivnH010hntHLsv/ZyVhC4pJotnLey/xDAdJlb/Ap6EgKjnGZpfMGL6OpYsec9RNPpUguT0bv5aqT+RpjGfSN97SblUGG47kjAw/RbnsuvCWjRXLwXHaYxdRgMq2HIP7+pSf7IQRr0ouAa7SUN9fr/9l1Ypito7cT1cLl5U2KBSsFFZAgMBAAECggEAE+txXVpfQ/sY2tdZ/BoK7gnz1qWKOwvPm/d+RWYe20+3AXIbuATZGBvCTqMNU724MoB32wCe2Dt+hCn0/yj5ot2F+qN40F2jxONLMUfeUkxUvJe0etf6Q7cADe1meQIpN6tGbJdnjuugpW2ZE1/HxfDFufkzplQ0sJvFMlPdI8kQxuSftuhnIYPly1+sq+BHA8ttL/zdURAKVJYdVduHeUbqI9mSE0wGom1opMikskj6DnMTWRyJsPOKQwA7CiTyXfn1OMdDC9+CiL2vG6Ds55dBKwSzfY8UPcftY+TuFyGPPRSt+s5pZL6lvSiRr2/6HXO3+3WuHASYJdb9od4mAQKBgQDpHYiB1FijtveH1WZGA55XJ75+9QykjqEIqhmGqXSl78/PwTQaXti9dS9+BzQEDnP3Z6ZywivBa3IQ13QQz/17yNEINzdB8UHer1cA8htmeL6VY6dCXQe30pWAAgaFEDg9LUq6hVPIu4ywhOIiEkNLRHYpErmSaOk1cIjXz0BnGQKBgQDTS29DhepPnlaCpgcKYos6BI8yzcVrHIYosOFS3etmbilnNOrkjR3IoDKex6KBRlWycr6XljsOo0VTNKB5Tx+MZzv1cJXL6MeeFRxY77YMq5xKbDcohj1ldZv7iAhVv9vV+vI8Yo5hHrYt788bG6xsfKZQhmocTa7frb+ZF3/EQQKBgCqbFTn1X4X8iN0wqZKeUrBHOU/m5bqlvtTgke3ExucVH3wvKaCwORjXdCiqlF0xbwyGyysqRekCBEUDu9jeyst29o1z2guZVpqmnVY06cEezGZtYkKE0kZMnLpapGppfn3f60qP17JWZO5WOyZTBC2bg6UaIQSbXBRCTyByZqjxAoGAckECIzMQpojqIq+Acx8iRfcdL82RCQBdkzdCQDr8BWFgRgyZT4j3J/toI++zcdAEmv/tC68StDGZVQrKE46zcoqII4oDlkWDW3ny4CyO9n79fkjR8rnUDT7xX6wJRcT/LNALmJd6gg3HWUOpa/Ek5WzpyfVE/5UKEK21QyUv04ECgYB21bPBUygudipE1SYniqIuJS8bb61VneU+RAfPpxLsgwSImzD6993GysHei4MiRvoUQAI9FcCTJGC2q1+bs6Gh+/RN4UjcskP+9NIIO5rMsBmqZivXlYVKd7TZkA/f51rw3vIjVQNEtSZQcUfjV6x2q3ixIdAPNsU8eSmKwsCQUw==-----END PRIVATE KEY-----");
		GoogleCloudUser gc = gp.getCloudUser(map);
		GoogleCloudImagePlugin gi = new GoogleCloudImagePlugin("src/main/resources/private/clouds/google-cloud/cloud.conf");
		gi.getAllImages(gc);
	}
}
