package org.fogbowcloud.manager.core.models.orders;

import javax.persistence.*;

@Entity
@Table (name = "tb_user_data")
public class UserData {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false, unique = true)
	private Long id;

	public UserData() {
		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
