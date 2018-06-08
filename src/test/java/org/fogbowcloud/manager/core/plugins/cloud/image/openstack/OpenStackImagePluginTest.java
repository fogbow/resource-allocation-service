package org.fogbowcloud.manager.core.plugins.cloud.image.openstack;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.ImageException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONObject;
import org.junit.Test;

import com.google.gson.Gson;

public class OpenStackImagePluginTest {

	
	@Test
	public void testGetAllImages() throws ImageException  {
		
		Properties properties = new Properties();
		
		properties.put(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY, "https://cloud.lsd.ufcg.edu.br:9292");
		OpenStackImagePlugin plugin = new OpenStackImagePlugin(properties);
		
		Token token = new Token();
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("tenantId", "3324431f606d4a74a060cf78c16fcb21");
		token.setAttributes(attributes);
		token.setAccessId("gAAAAABbGtM0Eqd3aINFw6lX1Q91k4RDOS4nTd3hkUhymh81JfYa5UGJjkNfbN3WWLHPa-1oSofJG75H9RCMv3Mm1kLzrfdZLErL-rF2ZTOnAxT_iI6qzPyU2A7GmXl9JP12_WHuhVdxLZysxO3UTHryTCJjy-jOWNdvbXV5E6JfWp-SW49rmrH5WXvSLNxEK80Sn7oJgFMjZ3VmyiNh8-skfM01Uo0_8B8dCRfkFskpdu6QV2LzeZg");
		Map<String, String> s = plugin.getAllImages(token);
		JSONObject json = new JSONObject(s);
		System.out.println(json);
		
		Image image = plugin.getImage("dcb8d22c-2162-49dc-8f71-f5cb98dd096e", token);
		Gson gson = new Gson();
		String userJSONString = gson.toJson(image);
		System.out.println(userJSONString);
		
	}
	
	
	
}
