/* ---------------------------------------------------------------------------------------------------------------
• LP_Score
– children: LP_StaffGroup (TODO), LP_Staff

• TODO:
- overrides should be inherited from LP_Object (see implementation in LP_Block)
- alternatively, LP_Score should have a LP_ContextBlock, which implements the overrides
--------------------------------------------------------------------------------------------------------------- */
LP_Score : LP_Object {
	var <staves, <lpObj="Score";
	*new { |staves|
		^super.new.init(staves);
	}
	init { |argStaves|
		staves = argStaves;
		this.initDefaults;
	}
	initDefaults {
		this.numericTimeSignatures_(true);
		this.showTupletRatios_(true);
		this.tupletFullLength_(true);
	}
	at { |index|
		^staves[index];
	}
	depth {
		^1;
	}
	proportionalNotationDuration_ { |lp_Duration, strict=false|
		var schemeMoment;
		schemeMoment = "".catList(lp_Duration.pair.insert(1, "/"));
		this.set(\proportionalNotationDuration,
			LP_Set("Score.proportionalNotationDuration = #(ly:make-moment" + schemeMoment ++ ")"));
		this.override(\strictNoteSpacing, LP_Override("Score.SpacingSpanner.strict-note-spacing =" + strict.lpStr));
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Staff
- children: LP_Voice (TODO), LP_Measure
--------------------------------------------------------------------------------------------------------------- */
LP_Staff : LP_Object {
	var <measures, <lpObj="Staff";
	var <instrumentName, <shortInstrumentName;
	*new { |measures|
		^super.new.init(measures);
	}
	init { |argMeasures|
		measures = argMeasures;
		measures.do { |measure| measure.addParent(this) };
	}
	//!!! workaround
	parent {
		^nil;
	}
	//!!! workaround
	isTuplet {
		^false;
	}
	at { |index|
		^measures[index];
	}
	depth {
		^2;
	}
	nodes {
		^measures.collect { |measure| measure.nodes }.flat;
	}
	leaves {
		^measures.collect { |measure| measure.leaves }.flat;
	}
	children {
		^measures.collect { |measure| measure.children }.flat;
	}
	copySeries { |first, second, last|
		^this.selectBy(LP_Event).copySeries(first, second, last);
	}
	selectBy { |class|
		^LP_Selection(this.nodes).selectBy(class);
	}
	offsets {
		^this.selectBy(LP_Event).components.collect { |each| each.beatDuration }.offsets.drop(-1);
	}
	// move up to LP_Object (so LP_Meausure and other LP_FixedDurationContainers can inherit)
	notes {
		^this.selectBy(LP_PitchEvent).notes;
	}
	// move up to LP_Object (so LP_Meausure and other LP_FixedDurationContainers can inherit)
	noteNames {
		^this.selectBy(LP_PitchEvent).noteNames;
	}
	// move up to LP_Object (so LP_Meausure and other LP_FixedDurationContainers can inherit)
	notes_ { |notes|
		this.selectBy(LP_PitchEvent).notes_(notes);
	}
	attach { |attachment|
		this.selectBy(LP_Leaf)[0].attach(attachment);
	}
	instrumentName_ { |name|
		instrumentName = name;
		this.set(\instrumentName, LP_Set("Staff.instrumentName = #\"" ++ name.asString + "\""));
	}
	shortInstrumentName_ { |name|
		shortInstrumentName = name;
		this.set(\shortInstrumentName, LP_Set("Staff.shortInstrumentName = #\"" ++ name.asString + "\""));
	}
	/*
	http://www.lilypond.org/doc/v2.19/Documentation/notation/displaying-pitches#automatic-accidentals
	default, voice, modern, modern-cautionary, modern-voice, modern-voice-cautionary, piano, piano-cautionary
	neo-modern, neo-modern-cautionary, neo-modern-voice, neo-modern-voice-cautionary, dodecaphonic,
	dodecaphonic-no-repeat, dodecaphonic-first, teaching, no-reset, forget

	\accidentalStyle modern
	*/
	accidentalStyle_ { |name|
		this.functionCall(LP_FunctionCall(\accidentalStyle, name.asString));
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_RhythmicStaff
!!! NB: Abjad does not have a RhythmicStaff class -- it uses Staff.context_name = 'RhythmicStaff'
- children: LP_Voice, LP_Measure
--------------------------------------------------------------------------------------------------------------- */
LP_RhythmicStaff : LP_Staff {
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Voice should include a name argument for defining logical voices
• see: http://abjad.mbrsi.org/core_concepts/working_with_logical_voices.html
- children: LP_Measure
--------------------------------------------------------------------------------------------------------------- */
LP_Voice : LP_Staff {
}

