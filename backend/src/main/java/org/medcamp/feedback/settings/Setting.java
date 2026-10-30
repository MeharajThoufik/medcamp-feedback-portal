package org.medcamp.feedback.settings;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

/** One app-wide setting, stored as text. {@link SettingsService} gives them types. */
@Entity
@Table(name = "settings")
public class Setting {

	@Id
	@Column(name = "setting_key", length = 100)
	private String key;

	@Column(name = "setting_value", nullable = false, length = 500)
	private String value;

	@UpdateTimestamp
	@Column(nullable = false)
	private Instant updatedAt;

	protected Setting() {
	}

	public Setting(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
