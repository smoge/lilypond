/* ---------------------------------------------------------------------------------------------------------------
• LP_RhythmTreeLeaf
--------------------------------------------------------------------------------------------------------------- */
LP_RhythmTreeLeaf : LP_Leaf {
	var <>preProlatedDuration, <duration, <writtenDuration, <>isTiedToNext=false, <>isTiedToPrev=false;
	var <indicators, <spanners, <markups, <overrides; //!!! move up to LP_Object and inherit
	update { |argDuration|
		this.duration_(argDuration);
		if (this.isTiedToPrev) { this.prevLeaf.isTiedToNext_(true) };
	}
	duration_ { |argDuration|
		var preProlatedDurations, children, tempContainer;
		duration = argDuration; //!!! remove
		writtenDuration = argDuration;
		if (duration.isAssignable.not && { this.root != this }) { this.replace(LP_TieContainer(this)) };
	}
	clone {
		^this.deepCopy;
	}
	shallowClone {
		^this.deepCopy.removeAttachments;
	}
	cloneAttachmentsFrom { |leaf|
		leaf.indicators.do { |attachment| if (attachment.notNil) { this.attach(attachment) } };
		leaf.markups.do { |attachment| if (attachment.notNil) { this.attach(attachment) } };
		leaf.spanners.do { |attachment| if (attachment.notNil) { this.attach(attachment) } };
		this.isTiedToNext_(leaf.isTiedToNext).isTiedToPrev_(leaf.isTiedToPrev);
	}
	//!!! move up to LP_Object and inherit
	removeAttachments {
		indicators = markups = spanners = nil;
	}
	//!!! move up to LP_Object and inherit
	attach { |attachment|
		case
		{ attachment.isKindOf(LP_Indicator) } {
			indicators = indicators.add(attachment);
			attachment.component_(this);
		}
		{ attachment.isKindOf(LP_Markup) } {
			markups = markups.add(attachment);
			attachment.component_(this);
		}
		{ attachment.isKindOf(LP_Spanner) } {
			// do not attach spanner if an instance of the same type is already attached
			if (spanners.detect { |elem| elem.isKindOf(attachment.class) }.isNil) {
				spanners = spanners.add(attachment);
				if (attachment.isKindOf(LP_Tie)) { this.isTiedToNext_(true) };
			};
		};
	}
	//!!! move up to LP_Object and inherit
	detach { |attachment|
		var object;
		if (attachment.isKindOf(LP_Indicator)) {
			attachment = indicators.detect { |elem| elem == attachment };
			if (attachment.notNil) { indicators.remove(attachment) };
		} {
			attachment = spanners.detect { |elem| elem == attachment };
			if (attachment.notNil) { spanners.remove(attachment) };
		};
	}
	beatDuration {
		var prolation;
		prolation = this.parents.collect { |parent| parent.prolation }.reduce('*');
		^duration.beatDuration * prolation;
	}
	// for use in LP_Player and some mutation methods
	type {
		^this.class;
	}
	//!!! move up to LP_Object and inherit
	override { |key, value|
		if (overrides.isNil) { overrides = OrderedIdentitySet[] };
		overrides = overrides.add(key -> value);
	}

	isLastLeafInMeasure {
		^if (this.root.notNil) { this.root.leaves.last == this } { false };
	}
	nextLeaf {
		var siblings, index;
		siblings = this.root.leaves;
		index = siblings.indexOf(this);
		^if (index == siblings.lastIndex) {
			if (this.root.nextMeasure.notNil) { this.root.nextMeasure.leaves[0] } { nil };
		} { siblings[index + 1] };
	}
	prevLeaf {
		var siblings, index;
		siblings = this.root.leaves;
		index = siblings.indexOf(this);
		^if (index == 0) {
			if (this.root.prevMeasure.notNil) { this.root.prevMeasure.leaves.last } { nil };
		} { siblings[index - 1] };
	}
}
/* ---------------------------------------------------------------------------------------------------------------
LP_Note

a = LP_Measure([4, 4], [1, -1, 1], [61, [60, 64]]);
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_Note : LP_RhythmTreeLeaf {
	var <note, <noteName, <dynamic;
	var namedPitch;
	*new { |note=60, duration|
		^super.new.init(note, duration);
	}
	init { |argNote, argDuration|
		this.note_(argNote);
		if (argDuration.isKindOf(LP_Duration)) {
			duration = argDuration; //!!! update -- get rid of duration
			writtenDuration = argDuration;
		} { preProlatedDuration = argDuration.abs };
	}
	// midinote, LP noteName, or SC noteName
	note_ { |argNote|
		namedPitch = LP_NamedPitch(argNote);
		note = namedPitch.note;
		noteName = namedPitch.noteName;
	}
	noteName_ { |noteName|
		this.note_(noteName);
	}
	/*dynamic_ { |argDynamic|
		dynamic = argDynamic;
	}*/
	//!!! TODO
	writtenDuration_ { // arg: a LP_Duration
	}
}
/* ---------------------------------------------------------------------------------------------------------------
LP_Chord

a = LP_Measure([4, 4], [1, -1, 1], [61, [60, 64]]);
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_Chord : LP_RhythmTreeLeaf {
	var <notes, <noteNames, <dynamic;
	var namedPitches;
	*new { |notes, duration|
		^super.new.init(notes, duration);
	}
	init { |argNotes, argDuration|
		this.notes_(argNotes);
		if (argDuration.isKindOf(LP_Duration)) {
			duration = argDuration; //!!! update -- get rid of duration
			writtenDuration = argDuration;
		} { preProlatedDuration = argDuration.abs };
	}
	// midinote, LP noteName, or SC noteName
	notes_ { |argNotes|
		namedPitches = argNotes.collect { |note| LP_NamedPitch(note) };
		notes = namedPitches.collect { |namedPitch| namedPitch.note };
		noteNames = namedPitches.collect { |namedPitch| namedPitch.noteName };
	}
	noteNames_ { |noteNames|
		this.notes_(noteNames);
	}

	/*dynamic_ { |argDynamic|
		dynamic = argDynamic;
	}*/
	//!!! should at method return notes or a LP_VerticalMoment ??
	at { |indices|
		if (indices.isNumber) { indices = [indices] };
		^notes.atAll(indices);
	}
	//!!! TODO: similar implementation to LP_Grob:override
	tweak { |index|
		this.notYetImplemented;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
LP_Rest
--------------------------------------------------------------------------------------------------------------- */
LP_Rest : LP_RhythmTreeLeaf {
	*new { |duration|
		^super.new.init(duration);
	}
	init { |argDuration|
		if (argDuration.isKindOf(LP_Duration)) {
			duration = argDuration;
		} { preProlatedDuration = argDuration.abs };
	}
}
/* ---------------------------------------------------------------------------------------------------------------
LP_MultimeasureRest
--------------------------------------------------------------------------------------------------------------- */
LP_MultimeasureRest : LP_Rest {
}
/* ---------------------------------------------------------------------------------------------------------------
LP_Skip
--------------------------------------------------------------------------------------------------------------- */
LP_Skip : LP_Rest {
}
