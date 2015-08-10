package com.yazino.test.statistic;

import com.yazino.game.api.NewsEvent;
import com.yazino.platform.service.statistic.NewsEventPublisher;

public class BlackholeNewsEventPublisher implements NewsEventPublisher {

	@Override
	public void send(final NewsEvent... events) {
	}

}
