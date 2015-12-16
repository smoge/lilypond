/* ---------------------------------------------------------------------------------------------------------------
• LP_Object
--------------------------------------------------------------------------------------------------------------- */
LP_Object {
	var <overrides, <commands, <sets, <functionCalls, <comments, <attachments;
	override { |key, lp_Override|
		if (overrides.isNil) { overrides = IdentityDictionary[] };
		overrides[key] = lp_Override;
	}
	command { |lp_Command|
		if (commands.isNil) { commands = IdentityDictionary[] };
		commands[lp_Command.name] = lp_Command;
	}
	set { |key, lp_Set|
		if (sets.isNil) { sets = IdentityDictionary[] };
		sets[key] = lp_Set;
	}
	functionCall { |lp_FunctionCall|
		if (functionCalls.isNil) { functionCalls = IdentityDictionary[] };
		functionCalls[lp_FunctionCall.name] = lp_FunctionCall;
	}
	comment { |lp_Comment|
		if (comments.isNil) { comments = [] };
		comments = comments.add(lp_Comment);
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
	//!!! move the methods below into new LP_Component class ??
	// i.e. superclass of LP_Score, LP_Staff, LP_Voice, LP_Measure, etc.
	// bar numbers
	showBarNumbers_ { |bool|
		this.override(\showBarNumbers, LP_Override(this.lpObj ++ ".BarNumber.stencil =" + bool.lpStr));
	}
	// bar lines
	showBarLines_ { |bool|
		this.override(\showBarLines, LP_Override(this.lpObj ++ ".BarLine.stencil =" + bool.lpStr));
	}
	// time signatures
	showTimeSignatures_ { |bool|
		this.override(\showTimeSignatures, LP_Override(this.lpObj ++ ".TimeSignature.stencil =" + bool.lpStr));
	}
	numericTimeSignatures_ { |bool|
		var style;
		style = if (bool) { "numbered" } { "mensural" };
		this.override(\timeSignatureStyle, LP_Override(this.lpObj ++ ".TimeSignature.style = #'" ++ style));
	}
	// tuplets
	showTupletRatios_ { |bool|
		var str;
		str = if (bool) { "#tuplet-number::calc-fraction-text" };
		this.override(\showTupletRatios, LP_Override(this.lpObj ++ ".TupletNumber.text =" + str));
	}
	tupletFullLength_ { |bool|
		this.set(\tupletFullLength, LP_Set("Score.tupletFullLength =" + bool.lpStr));
		this.override(\tupletFullLength, LP_Override("Score.TupletBracket.full-length-to-extent =" + bool.lpStr));
	}
	// note heads
	showNoteHeads_ { |bool|
		this.override(\showNoteHeads, LP_Override(this.lpObj ++ ".NoteHead.transparent = " + bool.lpStr));
		this.override(\showLedgerLines, LP_Override(this.lpObj ++ ".NoteHead.no-ledgers = " + bool.lpStr));
	}
	// stems
	showStems_ { |bool|
		this.override(\showStems, LP_Override(this.lpObj ++ ".Stem.stencil = " + bool.lpStr));
	}
	stemDirection_ { |value| // \up or \down
		this.override(\stemDirection, LP_Override(this.lpObj ++ ".Stem.direction = #" ++ value.asString.toUpper));
	}
	// ties
	showTies_ { |bool|
		this.override(\showTies, LP_Override(this.lpObj ++ ".Tie.stencil = " + bool.lpStr));
	}
	// beams
	showBeams_ { |bool|
		this.override(\showBeams, LP_Override(this.lpObj ++ ".Beam.stencil = " + bool.lpStr));
	}
	// flags
	showFlags_ { |bool|
		this.override(\showFlags, LP_Override(this.lpObj ++ ".Flag.stencil = " + bool.lpStr));
	}
	// dots
	showDots_ { |bool|
		this.override(\showDots, LP_Override(this.lpObj ++ ".Dots.stencil = " + bool.lpStr));
	}
	// rests
	showRests_ { |bool|
		this.override(\showRests, LP_Override(this.lpObj ++ ".Rest.transparent = " + bool.lpStr));
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Command
allowable formatSlots: \after, \before, \closing, \opening, \right

LP_Command(\tupletFullLength).lpStr;

x = LP_Object();
x.tupletFullLength_(true);
x.sets.do { |e| e.lpStr.postln };
--------------------------------------------------------------------------------------------------------------- */
LP_Command {
	var <name, <formatSlot;
	*new { |name, formatSlot=\before|
		^super.new.init(name, formatSlot);
	}
	init { |argName, argFormatSlot|
		name = argName;
		formatSlot = argFormatSlot;
	}
	lpStr { |indent=0|
		var str;
		str = "\n\\" ++ name.asString;
		if (indent > 0) { str = str.replace("\n", "\n".catList("\t" ! indent)) };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_FunctionCall
allowable formatSlots: \after, \before, \closing, \opening, \right

LP_FunctionCall(\accidentalStyle, "default").lpStr;

LP_FunctionCall("tupletFullLenght = ##t").lpStr;

LP_Set("TupletBracket.full-length-to-extent = ##t").lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_FunctionCall {
	var <name, <args, <formatSlot;
	*new { |name, args, formatSlot=\before|
		^super.new.init(name, args, formatSlot);
	}
	init { |argName, argArgs, argFormatSlot|
		name = argName;
		args = argArgs;
		if (args.notNil && { args.isKindOf(Array).not }) { args = [args] };
		formatSlot = argFormatSlot;
	}
	lpStr { |indent=0|
		var str;
		str = ("\n\\" ++ name.asString);
		if (args.notNil) { str = str.scatList(args) };
		if (indent > 0) { str = str.replace("\n", "\n".catList("\t" ! indent)) };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Override

Abjad:
x = LP_Override(contextName, grobName, isOnce, propertyList, value).lpStr
x.reverStr;

x = LP_Override("Staff", "TextSpanner", ['bound-details', 'left', 'text'], 12, true);
x.lpStr;

x = LP_Override("Staff.TextSpanner.bound-details.left = 12", isOnce: true);
x.lpStr;
x.lpStr(3);
--------------------------------------------------------------------------------------------------------------- */
LP_Override {
	var string, isOnce;
	*new { |string, isOnce=false|
		^super.new.init(string, isOnce);
	}
	init { |argString, argIsOnce|
		string = argString;
		isOnce = argIsOnce;
	}
	lpStr { |indent=0|
		var str;
		str = "\n" ++ if (isOnce) { "\\once \\override" } { "\\override" } + string.asString;
		if (indent > 0) { str = str.replace("\n", "\n".catList("\t" ! indent)) };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Set
--------------------------------------------------------------------------------------------------------------- */
LP_Set : LP_Override {
	lpStr { |indent=0|
		var str;
		str = "\n" ++ if (isOnce) { "\\once \\set" } { "\\set" } + string.asString;
		if (indent > 0) { str = str.replace("\n", "\n".catList("\t" ! indent)) };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Tweak
--------------------------------------------------------------------------------------------------------------- */
LP_Tweak {
	var string;
	*new { |string|
		^super.new.init(string);
	}
	init { |argString|
		string = argString;
	}
	lpStr { |indent=0|
		var str;
		str = "\n\\tweak" + string.asString;
		if (indent > 0) { str = str.replace("\n", "\n".catList("\t" ! indent)) };
		^str;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Comment

x = LP_Comment("blah blah blah");
x.lpStr;
--------------------------------------------------------------------------------------------------------------- */
LP_Comment {
	var <string;
	*new { |string|
		^super.new.init(string);
	}
	init { |argString|
		string = argString;
	}
	lpStr { |indent=0|
		var str;
		str = "\n";
		indent.do { |i| str = str ++ "\t" };
		^(str ++ "%" + string);
	}
}

/*
// modelled after Abjad: LilyPondGrobOverride
LP_Override {
	var contextName, grobName, propertyList, value, isOnce;
	*new { |contextName, grobName, propertyList, value, isOnce=false|
		^super.new.init(contextName, grobName, propertyList, value, isOnce);
	}
	init { |argContextName, argGrobName, argPropertyList, argValue, argIsOnce|
		contextName = argContextName;
		grobName = argGrobName;
		propertyList = argPropertyList;
		value = argValue;
		isOnce = argIsOnce;
	}
parser { |obj|
		^switch(obj.class,
			String, { obj.quote },
			Symbol,  { obj.asString.tr($_, $-) }, //!!! a method is needed for quoting scheme objects (by lookup?)
			Integer, { obj.asString },
			Float, { obj.asString },
			True, { "#t" },
			False, { "#f" }
		);
	}
	lpStr { |indent=0|
		var str="\n";
		str = str ++ if (isOnce) { "\\once \\override" } { "\\override" };
		if (contextName.notNil) { str = str + contextName.asString ++ "." };
		str = str ++ grobName.asString ++ ".";

		str = str + "=" + value.asString;
		if (indent > 0) { str = str.replace("\n", "\n".catList("\t" ! indent)) };
		^str;
	}
}
*/
