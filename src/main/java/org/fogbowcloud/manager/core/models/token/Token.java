package org.fogbowcloud.manager.core.models.token;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.utils.DateUtils;
import org.fogbowcloud.manager.core.utils.JSONHelper;
import org.json.JSONException;
import org.json.JSONObject;

@Entity
@Table(name = "tb_token")
public class Token {
	
	private static final String EXPIRATION_DATE = "expirationDate";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	@Transient
	//@ElementCollection
	private Map<String, String> attributes;
	
	@Column
	private String accessId;
	
	@OneToOne
	private User user;
	
	@Transient
	private DateUtils dateUtils = new DateUtils();

	public Token() {

	}

	public Token(String accessId, User user, Date expirationTime, Map<String, String> attributes) {
		this.accessId = accessId;
		this.user = user;
		if (attributes == null) {
			this.attributes = new HashMap<String, String>();
		} else {
			this.attributes = attributes;
		}
		attributes.put(Token.EXPIRATION_DATE, String.valueOf(expirationTime.getTime()));
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
	
	

	public DateUtils getDateUtils() {
		return dateUtils;
	}

	public void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void setAccessId(String accessId) {
		this.accessId = accessId;
	}

	public void setUser(User user) {
		this.user = user;
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

	@Entity
	@Table(name = "tb_user")
	public static class User {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private String id;
		
		@Column(name = "name", nullable = false, unique = true)
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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
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
