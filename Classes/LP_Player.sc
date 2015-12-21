/* ---------------------------------------------------------------------------------------------------------------
// play a LP_Score
a = LP_Staff([LP_Measure([4, 8], [LP_Tuplet(3, [1,1,1]), LP_Tuplet(2, [1,1,1,1,1])])]);
a.selectBy(LP_Event).mask([2, 3, -1, 2]);
a.selectBy(LP_PitchEvent).notes_([61, 62, 63, 71]);
b = a.deepCopy;
b.selectBy(LP_PitchEvent).notes_([63, 61, 62, 64] + 2);
c = a.deepCopy;
c.selectBy(LP_PitchEvent).notes_([63, 61, 62, 64].reverse + 5);
x = LP_Score([a, b, c]);
LP_Player(x).playMIDI;
// LP_Player(x).play;

LP_EventList(x);

// play a LP_Measure, LP_Voice or LP_Staff
a = LP_Staff([LP_Measure([4, 8], [LP_Tuplet(3, [1,1,1]), LP_Tuplet(2, [1,1,1,1,1])])]);
a.selectBy(LP_Event).mask([2, 3, -1, 2]);
a.selectBy(LP_PitchEvent).notes_([61, 62, 63]);
LP_Player(a).play;

LP_File(x).write("test1.ly");


a = LP_Staff([LP_Measure([4, 4], [1, 1, 1, 1])]);
a.selectBy(LP_PitchEvent).notes_([61, 62, 63, 64]);
LP_Player(a, tempo: 160/60).play;
--------------------------------------------------------------------------------------------------------------- */
LP_Instrument {
	var <name, <isMono;
	*new { |name, isMono=false|
		^super.new.init(name, isMono)
	}
	init { |argName, argIsMono|
		name = argName.asSymbol;
		isMono = argIsMono;
	}
}

LP_Player {
	var <eventList, <instruments, <tempo;
	*new { |music, instruments, tempo=1|
		^super.new.init(music, instruments,tempo);
	}
	init { |music, argInstruments, argTempo|
		eventList = LP_EventList(music);
		instruments = argInstruments;
		tempo = argTempo;
	}
	// array of patterns where each item = an independent voice
	//!!! move this to LP_EventList::asPattern method ?
	patterns {
		var durs, type, midinotes, instrument;
		^eventList.collect { |voice, i|
			# durs, type, midinotes = voice.flop;
			if (instruments.notNil) {
				instrument = instruments[i];
				is (instrument.isMono) {
					Pmono(instrument.name, \dur, Pseq(durs), \midinote, Pseq(midinotes), \tempo, tempo);
				} {
					Pbind(\instrument, instrument.name, \dur, Pseq(durs), \midinote, Pseq(midinotes), \tempo, tempo);
				};
			} {
				Pbind(\dur, Pseq(durs), \midinote, Pseq(midinotes), \tempo, tempo);
			};
		};
	}
	play {
		//!!! bind instrument and pan settings here
		//!!! also settable tempo ??
		Ppar(this.patterns).play;
	}
}

LP_MIDIPlayer : LP_Player {
	play {
		var device="IAC Driver", port="IAC Bus 1", midiOut, voices;

		if (MIDIClient.initialized == false) { MIDIClient.init };
		midiOut = midiOut ?? { MIDIOut.newByName(device, port).latency_(0.05) };

		voices = this.patterns.collect { |pattern, i|
			Pbindf(pattern, \proto, (type: 'midi', midiout: midiOut, midicmd: 'noteOn', chan: i));
		};

		Ppar(voices).play;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_EventList
TODO:
- make LP_Event class
- once LP_Event class is made, LP_EventList is updated to contain a list of LP_Events

a = LP_Measure([4, 8], [LP_Tuplet(3, [1,1,1]), LP_Tuplet(2, [1,1,1,1,1])]);
LP_EventList(a).printAll; "";

a = LP_Staff([LP_Measure([4, 8], [LP_Tuplet(3, [1,1,1]), LP_Tuplet(2, [1,1,1,1,1])])]);
LP_EventList(a).printAll; "";

a = LP_Staff([LP_Measure([4, 8], [LP_Tuplet(3, [1,1,1]), LP_Tuplet(2, [1,1,1,1,1])])]);
b = a.deepCopy;
a.selectBy(LP_PitchEvent).notes_((61,61.5..80));
b.selectBy(LP_PitchEvent).notes_((80,79..40));
c = LP_Score([a, b]);
LP_EventList(c).printAll; "";
--------------------------------------------------------------------------------------------------------------- */
LP_EventList {
	var scoreObject;
	*new { |scoreObject|
		^super.new.init(scoreObject);
	}
	init { |argScoreObject|
		scoreObject = argScoreObject;
		switch (scoreObject.class,
			LP_Measure, { ^[LP_MeasureEventList(scoreObject)] },
			LP_Staff, { ^[LP_StaffEventList(scoreObject)] },
			LP_Score, { ^scoreObject.staves.collect { |staff| LP_StaffEventList(staff) } }
		);
	}
}

LP_MeasureEventList {
	var measure;
	*new { |measure|
		^super.new.init(measure);
	}
	init { |argMeasure|
		var note;
		measure = argMeasure;
		^measure.selectBy(LP_Event).components.collect { |event, i|
			note = switch(event.type, LP_Note, { event.note }, LP_Chord, { event.notes }, LP_Rest, \rest);
			[event.beatDuration, event.type, note];
		};
	}
}

LP_StaffEventList : LP_MeasureEventList {}