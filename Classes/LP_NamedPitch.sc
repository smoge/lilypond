/* ---------------------------------------------------------------------------------------------------------------
• LP_NamedPitch

x = LP_NamedPitch(60);
x.note;
x.noteName;

x = LP_NamedPitch("c4");
x.note;
x.noteName;

x = LP_NamedPitch("df'");
x.note;
x.noteName;

x = LP_NamedPitch("cs'");
x.note;
x.noteName;

x = LP_NamedPitch('C#4');
x.note;
x.noteName;

x = LP_NamedPitch('Db-6');
x.note;
x.noteName;

x = LP_NamedChord(["cs'", 'D4', 63]);
x.notes;
x.noteNames;
--------------------------------------------------------------------------------------------------------------- */
LP_NamedPitch {
	var <note, <noteName;
	*new { |pitch|
		^if (pitch.isString || pitch.isKindOf(Symbol)) {
			if (pitch.asString.findRegexp("[0-9]").notEmpty) {
				this.newByMidiNoteName(pitch);
			} { this.newByLPStr(pitch) }
		} { this.newByMidinote(pitch) };
	}
	*newByMidinote { |note|
		^super.new.init(note, LP_NoteLib.[note]);
	}
	*newByLPStr { |str|
		^super.new.init(LP_NoteLib.[str.asSymbol], str.asString);
	}
	*newByMidiNoteName { |str|
		str = LP_NamedPitchParser(str);
		^super.new.init(LP_NoteLib.[str.asSymbol], str.asString);
	}
	init { |argNote, argNoteName|
		note = argNote;
		noteName = argNoteName;
	}
	asLP_Note { |note|
		^LP_Note(note);
	}
}

LP_NamedChord {
	var <notes, <noteNames;
	*new { |array|
		^super.new.init(array);
	}
	init { |array|
		var namedPitches;
		namedPitches = array.collect { |note| LP_NamedPitch(note) };
		notes = namedPitches.collect { |each| each.note };
		noteNames = namedPitches.collect { |each| each.noteName };
	}
}

LP_NamedPitchParser {
	*new { |inStr|
		var noteName, modifier, register, dict, outStr;
		inStr = inStr.asString.toLower;

		// recurse if inStr contains more than one token
		if (inStr.contains(" ")) {
			outStr = "";
			inStr = inStr.separate { |char| char.isSpace }.collect { |each| each.stripWhiteSpace };
			inStr = inStr.select { |each| each.findRegexp("[a-zA-Z]").notEmpty };
			inStr.do { |each| outStr = outStr ++ LP_NamedPitchParser(each) ++ " " };
			^outStr;
		};

		dict = (
			/* // dutch
			'#': "is", '##': "isis", '+': "ih", '#+': "isih", 'b': "es", 'bb': "eses", '-': "eh", 'b-': "eseh",
			*/
			'#': "s", '##': "ss", '+': "qs", '#+': "tqs", 'b': "f", 'bb': "ff", '-': "qf", 'b-': "tqf",
			'0': ",,,", '1': ",,", '2': ",", '3': "", '4': "'", '5': "''", '6': "'''", '7': "''''",
			'8': "'''''", '9': "''''''"
		);

		noteName = inStr[0];
		modifier = inStr[1..].findRegexp("[\#b\+\-]+");
		modifier = if (modifier.notEmpty) { modifier[0][1].asSymbol } { nil };
		register = inStr[1..].findRegexp("[0-9]")[0][1].asSymbol;

		outStr = if (modifier.notNil) {
			noteName ++ dict[modifier] ++ dict[register];
		} { noteName ++ dict[register] };

		^outStr;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_StringParser
- used in LP_Measure initialisation and by LP_Selection::notes_
- tokens must be separated by spaces

LP_StringParser("ef' <c' eqs' gs' bf'>").printAll; ""
LP_StringParser("Eb4 <C4 E+4 G#4 Bb4>").printAll; ""
--------------------------------------------------------------------------------------------------------------- */
LP_StringParser {
	*new { |string|
		var out, indices, chord;
		out = [];
		indices = string.findRegexp("<[a-zA-Z0-9\'\,\ #+\-]*>"); // get chords
		indices.do { |each|
			chord = each[1].separate { |char| char.isSpace };
			chord = chord.collect { |str| str.replace("<", "").replace(">", "").stripWhiteSpace };
			chord = chord.collect  { |str| LP_NamedPitch(str).noteName };
			out = out.add([each[0], chord]);
		};
		indices.do { |each| string = string.replace(each[1], "@" ! each[1].size) }; // remove chords from string
		indices = string.findRegexp("[a-zA-Z]+[\'\,\ #+\-]*[0-9]*"); // get notes
		indices.do { |each| out = out.add([each[0], LP_NamedPitch(each[1]).noteName]) };
		out = out.sort { |a, b| a[0] < b[0] }.flop[1];
		^out;
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_NoteLib
- when microtonal.ily is included (dutch, eight-tone resolution):
'__eses', // double-flat
'__esqq', // seven-e-flat
'__eseh', // three-q-flat
'__eseq', // flat-lower
'__es',   // flat
'__esiq', // flat-raise
'__eh',   // semi-flat
'__eq',   // natural-lower
'__',     // natural
'__iq',   // natural-raise
'__ih',   // semi-sharp
'__iseq', // sharp-lower
'__is',   // sharp
'__isiq', // sharp-raise
'__isih', // three-q-sharp
'__isqq', // seven-e-sharp
'__isis', // double-sharp
--------------------------------------------------------------------------------------------------------------- */
LP_NoteLib {
	classvar <lib1, <lib2;
	*at { |key| // two-way lookup
		// round to eigth-tones when microtonal.ily is included
		// if (key.isNumber) { ^lib1[key.round(0.25)][1] } { ^lib2[key.asSymbol] };
		// round to quarter-tones when microtonal.ily is not included
		if (key.isNumber) { ^lib1[key.round(0.5)][1] } { ^lib2[key.asSymbol] };
	}
	*initClass {
		var registers, registerOffset, noteNames, noteOffset, modifiers, modifierOffset;
		var lpStr, midinum;

		lib1 = (); // midinote in eighth-tone steps mapped to array of lilypond strings
		lib2 = (); // lilypond strings mapped to midinote

		registers = #[",,,",",,",",","","'","''","'''","''''","'''''","''''''"];
		registerOffset = (12, 24 .. 120);
		noteNames = #["c","d","e","f","g","a","b"];
		noteOffset = #[0,2,4,5,7,9,11];
		modifiers = #["ff", "tqf", "f", "qf", "", "qs", "s", "tqs", "ss"];
		modifierOffset = (-2, -1.5 .. 2);

		/* // dutch: including eighth-tones
		modifiers = #["eses","esqq","eseh","eseq","es","esiq","eh","eq","","iq","ih","iseq","is",
			"isiq","isih","isqq","isis"];
		modifierOffset = (-2, -1.75 .. 2);
		*/

		registers.do { |register, i|
			noteNames.do { |noteName, j|
				modifiers.do { |modifier, k|
					lpStr = noteName ++ modifier ++ register;
					midinum = registerOffset[i] + noteOffset[j] + modifierOffset[k];
					if (lib1[midinum].isNil) { lib1[midinum] = [lpStr] } {
						lib1[midinum] = lib1[midinum].add(lpStr);
					};
					lib2[lpStr.asSymbol] = midinum;
				};
			};
		};
	}
}