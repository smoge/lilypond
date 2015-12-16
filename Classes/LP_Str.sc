/* ---------------------------------------------------------------------------------------------------------------
• LP_RhythmTreeLeaf
--------------------------------------------------------------------------------------------------------------- */
+ LP_RhythmTreeLeaf {
	//!!! TODO: automatic generation of correct number of tab stops
	isFirstLeafIn { |spanner|
		^spanner.components.first == this;
	}
	isLastLeafIn { |spanner|
		^spanner.components.last == this;
	}
	formatStr1 { |str, inStr|
		// don't include space if inStr includes non-alphanumeric characters
		str = str ++ if (inStr.findRegexp("[\\[\\]\\<\\>\\(\\)\\!]").notEmpty) { inStr } { " " ++ inStr };
		^str;
	}
	formatStr3 { |str, inStr|
		if (this.depth.notNil) { (this.depth + 2).do { str = str ++ "\t" } };
		str = str ++ inStr;
		str = str ++ "\n";
		^str;
	}
	formatStr { |token|
		var str="";
		/* -------------------------------------------------------------------------------------------------------
		overrides for this leaf
		------------------------------------------------------------------------------------------------------- */
		/*overrides.do { |assoc|
			str = str ++ "\n\t\\once \\override" + assoc.key + "=" + assoc.value.asString ++ "\n";
		};*/
		overrides.do { |override| str = str ++ override.lpStr(this.depth + 2) };
		/* -------------------------------------------------------------------------------------------------------
		overrides for any attachments (markups, indicators, spanners)
		------------------------------------------------------------------------------------------------------- */
		(markups ++ indicators).do { |obj|
			obj.overrides.do { |override|
				if (override.isKindOf(LP_Tweak).not) { str = str ++ override.lpStr(this.depth + 2) };
			};
		};

		spanners.do { |spanner|
			if (spanner.overrides.notNil && { this.isFirstLeafIn(spanner) }) {
				spanner.overrides.do { |override| str = this.formatStr3(str, override.lpStartStr) };
			};
		};
		/* -------------------------------------------------------------------------------------------------------
		indicators that must be inserted before component string (clefs, tempo, etc.)
		------------------------------------------------------------------------------------------------------- */
		indicators.do { |indicator|
			if (indicator.position == \before) {
				//!!! move indentation formatiting into indicator's lpStr method ??
				if (this.depth.notNil) { (this.depth + 2).do { str = "\t" ++ str } };
				str = str ++ indicator.lpStr;
				str = "\n" ++ str;
			};
		};
		/* -------------------------------------------------------------------------------------------------------
		leaf string (note/rest/chord token)
		------------------------------------------------------------------------------------------------------- */
		str = str ++ "\n";
		if (this.depth.notNil) {
			if (this.parent.isKindOf(LP_TieContainer)) {
				(this.depth + 1).do { str = str ++ "\t" };
			} {
				(this.depth + 2).do { str = str ++ "\t" };
			};
		};
		str = str ++ token;
		/* -------------------------------------------------------------------------------------------------------
		indicators, markups, spanners attached to this leaf
		------------------------------------------------------------------------------------------------------- */
		markups.do { |markup| str = str + markup.lpStr(indent: this.depth + 2) };

		indicators.do { |indicator|
			if (indicator.position != \before) { str = str + indicator.lpStr };
		};

		spanners.do { |spanner|
			// LP_ComplexSpanners (glissando and tie): lpStr attached to all leaves but last
			if (spanner.isKindOf(LP_ComplexSpanner)) {
				if (this.isLastLeafIn(spanner).not) { str = this.formatStr1(str, spanner.lpStr) };
			} {
				if (this.isFirstLeafIn(spanner)) { str = this.formatStr1(str, spanner.lpStartStr) };
				if (this.isLastLeafIn(spanner)) { str = this.formatStr1(str, spanner.lpEndStr) };
			};
		};

		^str;
	}
}

+ LP_Note {
	lpStr {
		var str="";
		str = str ++ this.formatStr(noteName ++ writtenDuration.lpStr);
		if (isTiedToNext) { str = str + "~" };
		^str;
	}
}

// behaves like a leaf
//!!! TODO: overrides etc. are being written for every child -- they should only be written once
+ LP_TieContainer {
	lpStr {
		var str="";
		children.drop(-1).do { |child| child.isTiedToNext_(true) };
		children.do { |child| str = str ++ child.lpStr };
		if (isTiedToNext) { str = str + "~" };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• TEMP

- add noteHeads to LP_TieContainer
- add attach to LP_TieContainer

a = LP_Measure([4, 4], [1, -1, 4, 1], [62, [60, 65]]);
//a = LP_Measure([4, 4], [1, -1, 5, 1], [62, 60]);
a[2].noteHeads[0].color_(Color.black).style_('s2la');
a[2].noteHeads[1].style_(\harmonic);
a[2].attach(LP_Articulation('>'));
LP_File(LP_Score([LP_Staff([a])])).write(openPDF: true);
--------------------------------------------------------------------------------------------------------------- */
+ LP_Chord {
	lpStr {
		var str;
		if (this.noteHeads.select { |noteHead| noteHead.overrides.notNil }.notEmpty) {
			str = str ++ "<";
			noteNames.do { |noteName, i|
				noteHeads[i].overrides.do { |override| str = str ++ override.lpStr(this.depth + 2) };
				str = str ++ "\n";
				if (this.depth.notNil) { (this.depth + 2).do { str = str ++ "\t" } };
				str = str ++ noteName;
			};
			str = str ++ "\n";
			if (this.depth.notNil) { (this.depth + 2).do { str = str ++ "\t"} };
			str = str ++ ">";
		} {
			str = "<" ++ noteNames.reduce('+') ++ ">";
		};
		str = this.formatStr(str ++ writtenDuration.lpStr);
		if (isTiedToNext) { str = str + "~" };
		^str;
	}
}

+ LP_Rest {
	lpStr {
		var str="";
		str = str ++ this.formatStr("r" ++ duration.lpStr);
		^str;
	}
}

+ LP_MultimeasureRest {
	lpStr {
		^this.formatStr("R" ++ duration.lpStr);
	}
}

+ LP_Skip {
	lpStr {
		^this.formatStr("s" ++ duration.lpStr);
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_FixedDurationContainer
--------------------------------------------------------------------------------------------------------------- */
+ LP_FixedDurationContainer {
	formatStr {
		var str="";
		// this hook must stay in the superclass: isTuplet can be true for LP_Measure
		if (isTuplet) {
			if (this.depth > 0) { str = str ++ "\n" };
			(this.depth + 2).do { str = str ++ "\t" };
			str = str ++ "\\tuplet " ++ tupletNumerator.asString ++ "/" ++ tupletDenominator.asString + "{";
		} {
			(this.depth + 2).do { str = str ++ "\t" };
			str = str ++ "{";
		};
		children.do { |child| str = str ++ child.lpStr };
		str = str ++ "\n";
		(this.depth + 2).do { str = str ++ "\t" };
		str = str ++ "}";
		^str;
	}
}

+ LP_Tuplet {
	lpStr {
		^this.formatStr;
	}
}

+ LP_Measure {
	lpStr {
		var str;
		if (this.prevMeasure.isNil || { this.prevMeasure.timeSignature.pair != timeSignature.pair }) {
			str = "\n\t\t\\time " ++ timeSignature.lpStr;
		};
		//commands.do { |command| str = str ++ "\n\t\t\\" ++ command.asString  };
		//overrides.do { |assoc| str = str ++ "\n\t\\override" + assoc.key + "=" + assoc.value.asString };
		indicatorsAtHead.do { |indicator| str = str ++ "\n\t\t" ++ indicator.lpStr };
		str = str ++ "\n" ++ this.formatStr;
		indicatorsAtTail.do { |indicator| str = str ++ "\n\t\t" ++ indicator.lpStr };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Object

x = LP_Object();
x.command(LP_Command('tupletFullLength'));
x.commands.do { |command| command.lpStr.postln };
--------------------------------------------------------------------------------------------------------------- */
+ LP_Object {
	indicatorStr {
		var str="";
		//!!!attachments.do { |attachment| str = str ++ "\n\t\\" ++ attachment.lpStr };
		commands.do { |command| str = str ++ command.lpStr(this.depth) };
		functionCalls.do { |functionCall| str = str ++ functionCall.lpStr(this.depth) };
		sets.do { |set| str = str ++ set.lpStr(this.depth) };
		overrides.do { |override| str = str ++ override.lpStr(this.depth) };
		^str;
	}
}

+ LP_Score {
	lpStr {
		var str;
		str = "\n\\score {\n\t<<";
		str = str ++ this.indicatorStr;
		str = str ++ staves.collect { |staff| staff.lpStr }.reduce('++');
		str = str ++ "\n\t>>\n}\n";
		^str;
	}
}

+ LP_Staff {
	lpStr {
		var str;
		str = "\n\t\\new Staff {";
		str = str ++ this.indicatorStr;
		str = str ++ measures.collect { |measure| measure.lpStr }.reduce('++');
		str = str ++ "\n\t}";
		^str;
	}
}

+ LP_RhythmicStaff {
	lpStr {
		var str;
		str = "\t\\new RhythmicStaff {";
		str = str ++ this.indicatorStr;
		str = "\n" ++ str;
		str = str ++ measures.collect { |measure| measure.lpStr }.reduce('++');
		str = str ++ "\n\t}";
		^str;
	}
}

+ LP_Voice {
	lpStr {
		var str;
		str = "\t\\new Voice {";
		//!!! str = str ++ "\\voiceOne"; // for correct positioning of stems, etc.
		str = str ++ this.indicatorStr;
		str = "\n" ++ str;
		str = str ++ measures.collect { |measure| measure.lpStr }.reduce('++');
		str = str ++ "\n\t}";
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_TimeSignature
--------------------------------------------------------------------------------------------------------------- */
+ LP_TimeSignature {
	lpStr {
		^(numerator.asString ++ "/" ++ denominator.asString)
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Duration
--------------------------------------------------------------------------------------------------------------- */
+ LP_Duration {
	lpStr {
		^switch(this.numDots,
			0, { denominator.asString },
			1, { (denominator / 2).asInteger.asString ++ "." },
			2, { (denominator / 4).asInteger.asString ++ ".." },
			//3, { (denominator / 8).asInteger.asString ++ "..." }
		);
	}
}