package com.uw.tracknwalk.model;

import lombok.Getter;

@Getter
public class Step {

	String html_instructions;
	Distance distance;
	Duration duration;
	Location start_location;
	Location end_location;
	Polyline polyline;
}
