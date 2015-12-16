/* ---------------------------------------------------------------------------------------------------------------
• LP_Indicator

All lilypond layout objects: http://www.lilypond.org/doc/v2.18/Documentation/internals/all-layout-objects

TODO (from abjad):
LP_Accelerando
LP_Annotation
LP_Arrow
LP_BendAfter
LP_BowContactPoint
LP_BowMotionTechnique
LP_BowPressure
LP_ClefInventory
LP_IndicatorExpression
LP_IsAtSoundingPitch
LP_IsUnpitched
LP_KeyCluster
LP_KeySignature
LP_LineSegment
LP_MetricModulation
LP_PageBreak
LP_Ritardando
LP_StaffChange
LP_StringContactPoint
LP_StringNumber
LP_SystemBreak
LP_TempoInventory
LP_TimeSignatureInventory
LP_Tremolo
LP_Tuning

position: \above, \below


"\\markup { \\override #'(circle-padding . 0.25) \\circle \\finger 4 }"

a = LP_Measure([5, 8], [1, 3, LP_Tuplet(7, [1, 2, 1, 2, 2]), 3], (60..67));
a.selectBy(LP_Event)[1].attach(LP_StemTremolo(4));
a.selectBy(LP_Event).at([2,3]).attach(LP_Dynamic('sf').color_(Color.grey));
a.selectBy(LP_Event).at([0,5]).attach(LP_Articulation('flageolet').padding_(3).priority_(9999));
a.selectBy(LP_Event).at([2,3]).attach(LP_Articulation('^').color_(Color.red));
x = LP_Score([b = LP_Staff([a])]);
x.showTupletRatios_(true);
LP_File(x).write;
--------------------------------------------------------------------------------------------------------------- */
LP_Indicator : LP_Object {
	var <string, <>position, <halign, <padding, <color, <priority;
	var <>component;
	*new { |string, position|
		^super.new.init(string, position);
	}
	init { |argString, argPosition|
		string = argString;
		position = argPosition;
	}
	padding_ { |value|
		padding = value;
		this.override(\padding,  LP_Override(this.grobName ++ ".padding =" + padding, true));
	}
	halign_ { |value| // \left, \center, \right, or a number in the range -1..1
		halign = value;
		this.override(\halign, LP_Override(this.grobName ++ ".self-alignment-X = #" ++ halign.asString.toUpper, true));
	}
	priority_ { |value|
		priority = value;
		this.override(\priority, LP_Override(this.grobName ++ ".outside-staff-priority =" + priority, true));
	}
	color_ { |argColor|
		var rgb, color;
		color = argColor;
		rgb = [color.red, color.green, color.blue];
		this.override(\color, LP_Override(this.grobName ++ ".color = #(rgb-color".scatList(rgb) ++ ")", true));
	}
	lpStr {
		^(switch(position, \above, "^", \below, "_", nil, "-") ++ string);
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_NoteHead [http://lilypond.org/doc/v2.18/Documentation/notation/note-heads]
LP_NoteHead(\default).lpStr;

[http://lilypond.org/doc/v2.18/Documentation/notation/note-head-styles]
default
altdefault
baroque
neomensural
mensural
petrucci
harmonic
harmonic-black
harmonic-mixed
diamond
cross
xcircle
triangle
slash

rectangle-black
\tweak NoteHead.stencil #ly:text-interface::print
\tweak NoteHead.text \markup { \musicglyph #"noteheads.s2la" }
--------------------------------------------------------------------------------------------------------------- */
LP_NoteHead : LP_Indicator {
	classvar <noteHeads;
	var <type, <lpObj="NoteHead", <grobName="NoteHead";
	*initClass {
		noteHeads = #['default', 'harmonic', 'cross'];
	}
	*new { |type=\default|
		^super.new.init(type);
	}
	init { |argType|
		type = argType;
	}
	style_ { |argType| // \harmonic or \xNote (cross)
		type = argType;
		if (component.isKindOf(LP_Note)) {
			this.override(\style, LP_Override(this.grobName ++ ".style = #'" ++ type.asString, true));
		} {
			this.override(\style, LP_Tweak(this.grobName ++ ".style #'" ++ type.asString, true));
		};
	}
	color_ { |argColor|
		var rgb, color;
		color = argColor;
		rgb = [color.red, color.green, color.blue];
		if (component.isKindOf(LP_Note)) {
			this.override(\color, LP_Override(this.grobName ++ ".color = #(rgb-color".scatList(rgb) ++ ")", true));
		} {
			this.override(\color, LP_Tweak(this.grobName ++ ".color #(rgb-color".scatList(rgb) ++ ")", true));
		};
	}
	lpStr {
		^"";
	}

	// Abjad methods
	writtenPitch {
	}
	note {
	}
	isCautionary {
	}
	isForced {
	}
	isParenthesized {
	}
	tweak {
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Dynamic [http://www.lilypond.org/doc/v2.18/Documentation/internals/dynamictext]
LP_Dynamic('mp').lpStr;
- grobName (vertical): DynamicLineSpanner
- grobName (horizontal): DynamicText
--------------------------------------------------------------------------------------------------------------- */
LP_Dynamic : LP_Indicator {
	var <lpObj="Dynamic", <grobName="DynamicLineSpanner";
	lpStr {
		^(switch(position, \above, "^", \below, "_", nil, "-") ++ "\\" ++ string);
	}
	halign_ { |value|
		halign = value;
		this.override(\halign, LP_Override("DynamicText.self-alignment-X = #" + halign.asString.toUpper, true));
	}
	color_ { |argColor|
		var rgb, color;
		color = argColor;
		rgb = [color.red, color.green, color.blue];
		this.override(\priority, LP_Override("DynamicText.color = #(rgb-color".scatList(rgb) ++ ")", true));
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Articulation
LP_Articulation('>').lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_Articulation : LP_Indicator {
	classvar <articulations;
	var <lpObj="Articulation", <grobName="Script";
	*initClass {
		articulations = #[
			'>', '^', '_', '!', '.', '-', '+', 'accent', 'espressivo', 'marcato', 'portato', 'staccatissimo',
			'staccato', 'tenuto', 'prall', 'prallup', 'pralldown', 'upprall', 'downprall', 'prallprall',
			'lineprall','prallmordent', 'mordent', 'upmordent','downmordent', 'trill', 'turn', 'reverseturn',
			'shortfermata', 'fermata', 'longfermata', 'verylongfermata', 'upbow', 'downbow', 'flageolet',
			'open', 'halfopen', 'lheel', 'rheel', 'ltoe', 'rtoe', 'snappizzicato', 'stopped', 'segno',
			'coda', 'varcoda', 'accentus', 'circulus', 'ictus', 'semicirculus', 'signumcongruentiae'
		];
	}
	*new { |string, position|
		^if (articulations.includes(string.asSymbol)) {
			super.new(string, position);
		} { error("Articulation" + string.asString.quote + "not found.") };
	}
	lpStr {
		var str;
		str = switch(position, \above, "^", \below, "_", nil, "-");
		if (string.asString[0].isAlpha) { str = str ++ "\\" };
		str = str ++ string.asString;
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Clef [http://www.lilypond.org/doc/v2.18/Documentation/internals/clef]
http://www.lilypond.org/doc/v2.19/Documentation/notation/clef-styles

LP_Clef('bass').lpStr;
LP_Clef('treble^8').lpStr;
LP_Clef.clefs.printAll;
--------------------------------------------------------------------------------------------------------------- */
LP_Clef : LP_Indicator {
	classvar <clefs;
	var <type, <lpObj="Clef", <grobName="Clef";
	*initClass {
		clefs = #[
			'G', 'G2', 'treble', 'violin', 'french', 'GG', 'tenorG', 'soprano', 'mezzosoprano', 'C', 'alto',
			'tenor', 'baritone', 'varC', 'altovarC', 'tenorvarC', 'baritonevarC', 'varbaritone', 'baritonevarF',
			'F', 'bass', 'subbass', 'percussion'
		];
	}
	*new { |type=\treble|
		^super.new.init(type).position_(\before);
	}
	init { |argType|
		type = argType;
	}
	lpStr {
		string = type.asString;
		if (string.findRegexp("[^a-zA-Z]").notEmpty) { string = string.quote }; // quote "treble^8" etc.
		^("\\clef" + string);
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Arpeggio [http://www.lilypond.org/doc/v2.18/Documentation/internals/arpeggio]

a = LP_Measure([4, 4], [1], "<C4 E4 G4 Bb4>");
a[0].attach(LP_Arpeggio().direction_(\up).positions_(-3, 1));
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_Arpeggio : LP_Indicator {
	var <direction, <lpObj="Arpgeggio", <grobName="Arpeggio";
	direction_ { |argDirection| // \up or \down
		direction = argDirection;
		this.override(\direction,
			LP_Override(this.grobName ++ ".arpeggio-direction = #" ++ direction.asString.toUpper, true);
		);
	}
	positions_ { |bottom=0, top=0|
		this.override(\positions,
			LP_Override(this.grobName ++ ".positions = #'(" ++ bottom + "." + top ++")", true);
		);
	}
	lpStr {
		^switch(position, \above, "^", \below, "_", nil, "-") ++ "\\arpeggio";
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_LaissezVibrer []

a = LP_Measure([4, 4], [1], "<C4 E4 G4 Bb4>");
a[0].attach(LP_LaissezVibrer());
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_LaissezVibrer : LP_Indicator {
	var <lpObj, <grobName;
	lpStr {
		^"\\laissezVibrer";
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_BreathMark []

a = LP_Measure([4, 4], [1], "<C4 E4 G4 Bb4>");
a[0].attach(LP_BreathMark());
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_BreathMark : LP_Indicator {
	var <lpObj, <grobName;
	lpStr {
		^"\\breathe";
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Fermata []

a = LP_Measure([4, 4], [1], "<C4 E4 G4 Bb4>");
a[0].attach(LP_Fermata());
LP_File(LP_Score([LP_Staff([a])])).write;

a = LP_Measure([4, 4], [1], "<C4 E4 G4 Bb4>");
a[0].attach(LP_Fermata(\long));
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_Fermata : LP_Indicator {
	var <type, <lpObj, <grobName;
	*new { |type=\default|
		^super.new.init(type);
	}
	init { |argType|
		type = argType;
		string = switch(type,
			\default, "\\fermata",
			\short, "\\shortfermata",
			\long, "\\longfermata",
			\verylong, "\\verylongfermata"
		);
	}
	lpStr {
		^string;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_StemTremolo []
LP_StemTremolo(3).lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_StemTremolo : LP_Indicator {
	var <numFlags, <lpObj, <grobName;
	*new { |numFlags=3|
		^super.new.init(numFlags);
	}
	init { |argNumFlags|
		numFlags = argNumFlags;
	}
	lpStr {
		^(":" ++ Array.geom(6, 8, 2)[numFlags - 1].asString);
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_ColorFingering []

a = LP_Measure([4, 4], [1], "C4");
a[0].attach(LP_ColorFingering(5, \above).halign_(\right)); // halign_ not working
LP_File(LP_Score([LP_Staff([a])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_ColorFingering : LP_Indicator {
	var <lpObj, <grobName="Script";
	lpStr {
		var str;
		str = "\\markup { \\override #'(circle-padding . 0.25) \\circle \\finger" + string.asString.quote + "}";
		^switch(position, \above, "^", \below, "_", nil, "-") ++ str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_StringNumber []
http://www.lilypond.org/doc/v2.18/Documentation/internals/stringnumber

--------------------------------------------------------------------------------------------------------------- */
LP_StringNumber : LP_Indicator {
	var <number, <style, <lpObj, <grobName="Script";
	*new { |number, style=\arabic| // \arabic or \roman
		^super.new.init(number, style);
	}
	init { |argNumber, argStyle|
		number = argNumber;
		style = argStyle;
	}
	lpStr {
		//!! TODO - use custom markup
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_MetronomeMark [http://www.lilypond.org/doc/v2.18/Documentation/internals/metronomemark]
LP_MetronomeMark(LP_Duration(3, 8), 60).lpStr;
LP_MetronomeMark(LP_Duration(1, 4), "120-140").lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_MetronomeMark : LP_Indicator {
	var <duration, <bpm;
	var <lpObj="MetronomeMark", <grobName="MetronomeMark";
	*new { |duration, bpm|
		^super.new.init(duration, bpm)
	}
	init { |argDuration, argBpm|
		duration = argDuration;
		bpm = argBpm;
	}
	lpStr {
		^("\\tempo" + duration.lpStr + "=" + bpm.asString)
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_RehearsalMark [http://www.lilypond.org/doc/v2.18/Documentation/internals/rehearsalmark]
[http://www.lilypond.org/doc/v2.17/Documentation/notation/bars#rehearsal-marks]
LP_RehearsalMark().lpStr;
LP_RehearsalMark("A").lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_RehearsalMark : LP_Indicator {
	var <lpObj="RehearsalMark", <grobName="RehearsalMark";
	lpStr {
		^if (string.isNil) { "\\mark \\default" } { "\\mark" + string.asString.quote };
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_BarLine [http://www.lilypond.org/doc/v2.18/Documentation/internals/barline]
[http://www.lilypond.org/doc/v2.17/Documentation/notation/bars]
LP_BarLine().lpStr;
LP_BarLine(\final).lpStr;
LP_BarLine(\doubleRepeat).lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_BarLine : LP_Indicator {
	var <type, <lpObj="BarLine";
	*new { |type=\default|
		^super.new.init(type);
	}
	init { |argType|
		type = argType;
		string = switch(type,
			\default, "|",
			\final, "|.",
			\double, "||",
			\solid, ".",
			\dotted, ";",
			\dashed, "!",
			\tick, "'",
			\startRepeat, ".|:",
			\endRepeat, ":|.",
			\dualRepeat, ":..:",
			\doubleDualRepeat, ":|.|:"
		);
	}
	lpStr {
		^("\\bar" + string.quote);
	}
}
