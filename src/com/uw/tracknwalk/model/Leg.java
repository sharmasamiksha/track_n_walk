package com.uw.tracknwalk.model;

import lombok.Getter;

@Getter
public class Leg {

	Step [] steps;
	Distance distance;
	Duration duration;
	String start_address;
	String end_address;
	Location start_location;
	Location end_location;
	Time departure_time;
	Time arrival_time;
}
