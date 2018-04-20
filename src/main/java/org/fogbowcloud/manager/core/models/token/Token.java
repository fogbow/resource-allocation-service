package org.fogbowcloud.manager.core.models.token;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.fogbowcloud.manager.core.utils.DateUtils;
import org.fogbowcloud.manager.core.utils.JSONHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class Token {

	private static final String EXPIRATION_DATE = "expirationDate";

	private Map<String, String> attributes;
	private String accessId;
	private User user;
	private DateUtils dateUtils = new DateUtils();

	public Token(String accessId, User user, Date expirationTime, Map<String, String> attributes) {
		this.accessId = accessId;
		this.user = user;
		if (attributes == null) {
			this.attributes = new HashMap<>();
		} else {
			this.attributes = attributes;
		}

		this.attributes.put(Token.EXPIRATION_DATE, String.valueOf(expirationTime.getTime()));
	}

	public String get(String attributeName) {
		return attributes.get(attributeName);
	}

	public String getAccessId() {
		return this.accessId;
	}

	public Date getExpirationDate() {
		String dataExpiration = this.attributes.get(Token.EXPIRATION_DATE);
		if (dataExpiration != null) {
			return new Date(Long.parseLong(dataExpiration));
		} else {
			return null;
		}
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public boolean isExpiredToken() {
		long expirationDateMillis = getExpirationDate().getTime();
		return expirationDateMillis < dateUtils.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "Token [attributes=" + attributes + ", " + ", user=" + user + "]";
	}

	public User getUser() {
		return this.user;
	}

	public JSONObject toJSON() throws JSONException {
		return new JSONObject().put("access_id", accessId).put("user", user != null ? user.toJSON() : null)
				.put("attributes", attributes != null ? attributes.toString() : null);
	}

	public static Token fromJSON(String jsonStr) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonStr);
		String accessId = jsonObject.optString("access_id");
		JSONObject userJson = jsonObject.optJSONObject("user");
		return new Token(!accessId.isEmpty() ? accessId : null,
				userJson != null ? User.fromJSON(userJson.toString()) : null, null,
				JSONHelper.toMap(jsonObject.optString("attributes")));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Token other = (Token) obj;
		if (accessId == null) {
			if (other.accessId != null)
				return false;
		} else if (!accessId.equals(other.accessId))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (attributes != null
				&& !new HashSet(attributes.values()).equals(new HashSet(other.attributes.values())))
			return false;
		if (dateUtils == null) {
			if (other.dateUtils != null)
				return false;
		}
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	/**
	 * 
	 * id : required and unique name : required
	 *
	 */
	public static class User {

		private String id;
		private String name;

		public User(String id, String name) {
			if (id == null || name == null) {
				throw new IllegalArgumentException("User id (" + id + ") and " + "name (" + name + ") are not null.");
			}
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public JSONObject toJSON() throws JSONException {
			return new JSONObject().put("id", this.id).put("name", this.name);
		}

		public static User fromJSON(String jsonStr) throws JSONException {
			JSONObject jsonObject = new JSONObject(jsonStr);
			return new User(jsonObject.optString("id"), jsonObject.optString("name"));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			User otherUser = (User) obj;
			if (!id.equals(otherUser.getId()) || !name.equals(otherUser.getName())) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "User [id=" + id + ", name=" + name + "]";
		}

	}

}
