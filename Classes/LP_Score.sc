/* ---------------------------------------------------------------------------------------------------------------
• LP_Object
--------------------------------------------------------------------------------------------------------------- */
LP_Object {
	var <overrides, <sets, <functions, <attachments;
	//!!! remove init1 -- subclasses add properties dynamically when needed
	init1 { // called by subclass init method
		overrides = OrderedIdentitySet[];
		sets = OrderedIdentitySet[];
		functions = OrderedIdentitySet[];
		attachments = [];
	}
	override { |property, value|
		if (overrides.isNil) { overrides = OrderedIdentitySet[] };
		overrides = overrides.add(property -> value);
	}
	set { |property, value|
		if (sets.isNil) { sets = OrderedIdentitySet[] };
		sets = sets.add(property -> value);
	}
	addFunction { |funcName, args|
		if (functions.isNil) { functions = OrderedIdentitySet[] };
		functions = functions.add(funcName -> args);
	}
	attach { |attachment|
		if (attachments.isNil) { attachments = [] };
		attachments = attachments.add(attachment);
	}
	style_ { |className|
		className.new(this);
	}
	// always create a new instance of the copied object
	copy {
		^this.deepCopy;
	}
}
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
	}
	at { |index|
		^staves[index];
	}
	proportionalNotationDuration_ { |lp_Duration, strict=false|
		var schemeMoment;
		schemeMoment = "".catList(lp_Duration.pair.insert(1, "/"));
		this.set('Score.proportionalNotationDuration', "#(ly:make-moment" + schemeMoment ++ ")");
		this.override('Score.SpacingSpanner.strict-note-spacing', if (strict) { "##t" } { "##f" });
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
		this.set('Staff.instrumentName', "#\"" ++ name.asString + "\"");
	}
	shortInstrumentName_ { |name|
		shortInstrumentName = name;
		this.set('Staff.shortInstrumentName', "#\"" ++ name.asString + "\"");
	}
	accidentalStyle_ { |name|
		this.addFunction('accidentalStyle', name);
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

