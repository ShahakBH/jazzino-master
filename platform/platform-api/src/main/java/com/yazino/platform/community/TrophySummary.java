package com.yazino.platform.community;

import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.ParameterisedMessage;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A dto object for sending trophy data to the front end.
 */
public class TrophySummary implements Serializable {
	private static final long serialVersionUID = 267652240213117337L;

	private final String name;
	private final String image;
	private final ParameterisedMessage message;
	private final int count;

	public TrophySummary(final String name,
                         final String image,
                         final ParameterisedMessage message,
                         final int count) {
		notNull(name, "name must not be null");
		notNull(image, "image must not be null");
		notNull(message, "message must not be null");
		this.name = name;
		this.image = image;
		this.message = message;
		this.count = count;
	}

	public String getName() {
		return name;
	}

	public ParameterisedMessage getMessage() {
		return message;
	}

	public String getImage() {
		return image;
	}

	public int getCount() {
		return count;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
