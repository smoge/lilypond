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
	override { |key, lp_Override|
		if (overrides.isNil) { overrides = IdentityDictionary[] };
		overrides[key] = lp_Override;
	}
	/*override { |key, value|
		if (overrides.isNil) { overrides = OrderedIdentitySet[] };
		overrides = overrides.add(key -> value);
	}*/

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
--------------------------------------------------------------------------------------------------------------- */
LP_Note : LP_RhythmTreeLeaf {
	var <note, <noteName, <noteHead, <dynamic;
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
		noteHead = LP_NoteHead();
		this.attach(noteHead);
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
--------------------------------------------------------------------------------------------------------------- */
LP_Chord : LP_RhythmTreeLeaf {
	var <notes, <noteNames, <noteHeads, <dynamic;
	var namedPitches, <tweaks;
	*new { |notes, duration|
		^super.new.init(notes, duration);
	}
	init { |argNotes, argDuration|
		this.notes_(argNotes);
		if (argDuration.isKindOf(LP_Duration)) {
			duration = argDuration; //!!! -- get rid of duration
			writtenDuration = argDuration;
		} { preProlatedDuration = argDuration.abs };
		noteHeads = LP_NoteHead() ! notes.size;
		indicators = indicators.reject { |elem| elem.isKindOf(LP_NoteHead) }; //!!! NOT WORKING! BUG!
		noteHeads.do { |noteHead, i| this.attach(noteHead) };
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
	//!!! TODO: similar implementation to LP_Grob:override
	tweak { |index, lp_Tweak|
		if (tweaks.isNil) { tweaks = [] };
		tweaks[index] = lp_Tweak;
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
LP_Skip (spacer rest)
--------------------------------------------------------------------------------------------------------------- */
LP_Skip : LP_Rest {
}
