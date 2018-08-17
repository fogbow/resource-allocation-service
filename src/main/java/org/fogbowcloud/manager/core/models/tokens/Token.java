package org.fogbowcloud.manager.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

//@Entity
//@Inheritance(strategy = InheritanceType.JOINED)
//@DiscriminatorColumn(name="descriminatorColumn")
@MappedSuperclass
public class Token {
	
	@Id
	@Column
    private String tokenValue;

    public Token() {}

    public Token(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenValue() {
        return this.tokenValue;
    }
}
