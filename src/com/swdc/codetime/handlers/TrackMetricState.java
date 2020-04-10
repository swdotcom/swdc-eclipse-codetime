package com.swdc.codetime.handlers;

import org.eclipse.core.commands.State;

import com.swdc.codetime.Activator;

public class TrackMetricState extends State {

	public TrackMetricState() {
	  setValue(Activator.SEND_TELEMTRY.get());
	}
	
	@Override
	public void setValue(Object value) {

		if(value instanceof Boolean)
			Activator.SEND_TELEMTRY.set((Boolean) value);
		super.setValue(value);
		
	}
	
	@Override
	public Object getValue() {
		
		return super.getValue();
	}

}
