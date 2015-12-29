/* ---------------------------------------------------------------------------------------------------------------
• LP_RhythmTreeContainer
--------------------------------------------------------------------------------------------------------------- */
LP_RhythmTreeContainer : LP_Container {
	var <preProlatedDuration, <duration, <innerDurations;
	var <isTuplet, <tupletNumerator, <tupletDenominator;
	*new { |duration, children|
		children = children.collect { |child|
			case
			{ child.isNumber && { child > 0 } } { LP_Note(60, child.asInteger).isTiedToPrev_(child.isFloat) }
			{ child.isNumber && { child < 0 } } { LP_Rest(child.asInteger) }
			{ child.isArray && { child[1].isArray } } { LP_Tuplet(*child) } // allow rhythm tree syntax
			{ child }; // child is already wrapped in LP_Note, LP_Chord, LP_Rest or LP_Tuplet
		};
		^super.new(children).init1(duration);
	}
	init1 { |duration|
		preProlatedDuration = duration ? 1;
		//if (this.isKindOf(LP_Measure)) { this.update(duration).rewriteTuplets };
		if (this.isKindOf(LP_Measure)) { this.update(duration) };
	}
	update { |argDuration|
		duration = if (this.isRoot) { preProlatedDuration } { argDuration };
		if (duration.notNil) { this.prolateInnerDurations(duration, children) };
		children.do { |child, i| child.update(innerDurations[i]) };
	}
	// assign: innerDurations, isTuplet, tupletNumerator, tupletDenominator
	prolateInnerDurations { |argDuration, children|
		var preProlatedDurations, baseDenominator, tupletRatio;

		preProlatedDurations = children.collect { |child| child.preProlatedDuration };
		tupletNumerator = preProlatedDurations.sum;
		tupletDenominator = argDuration.numerator;

		if (tupletNumerator < tupletDenominator) {
			while { tupletNumerator <= tupletDenominator } {
				preProlatedDurations = preProlatedDurations * 2;
				tupletNumerator = tupletNumerator * 2;
			};
		} {
			while { tupletDenominator <= (tupletNumerator / 2) } {
				tupletDenominator = tupletDenominator * 2;
			};
		};

		baseDenominator = ((argDuration.denominator * tupletDenominator) / argDuration.numerator).asInteger;
		innerDurations = preProlatedDurations.collect { |num| LP_Duration(num, baseDenominator) };
		isTuplet = (tupletNumerator != tupletDenominator); // can be true for LP_Measure and LP_Tuplet
		tupletRatio = [tupletNumerator, tupletDenominator];
		# tupletNumerator, tupletDenominator = (tupletRatio / tupletRatio.reduce(\gcd)).asInteger;
	}
	prolation {
		^if (isTuplet) { (tupletDenominator / tupletNumerator) } { 1 };
	}
	//!!! move up to LP_Object and inherit
	// override dup: music trees always creates a new instance of the copied object
	dup { |n=2|
		^Array.fill(n, { this.deepCopy });
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_FixedDurationContainer
--------------------------------------------------------------------------------------------------------------- */
LP_FixedDurationContainer : LP_RhythmTreeContainer {
	*new { |duration, children, notes|
		^super.new(duration, children).notes_(notes);
	}
	selectBy { |class|
		^LP_Selection(this.nodes).selectBy(class);
	}
	// the beat-wise offsets of events within the container (1 = crotchet)
	offsets {
		^this.selectBy(LP_Event).components.collect { |each| each.beatDuration }.offsets.drop(-1);
	}
	beatDuration {
		^duration.beatDuration;
	}
	//!!! move up to LP_Object and inherit
	notes {
		^this.selectBy(LP_PitchEvent).notes;
	}
	//!!! move up to LP_Object and inherit
	noteNames {
		^this.selectBy(LP_PitchEvent).noteNames;
	}
	//!!! move up to LP_Object and inherit
	notes_ { |notes|
		if (notes.notNil) { this.selectBy(LP_PitchEvent).notes_(notes) };
	}

	rewrite {
		if (this.leaves.size == 1) {
			// TIDY THIS !!!
			if (children[0].isKindOf(LP_TieContainer)) {
				this.replace(children[0], children[0][0].preProlatedDuration_(this.preProlatedDuration));
			} {
				this.replace(children[0], children[0].preProlatedDuration_(this.preProlatedDuration));
			};
		};
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Measure
TODO:
- parentVoice: not a good inst var name, but parent causes naming conflicts in the tree, where LP_Measure must
always be root (and not the parent LP_Staff or LP_Voice) - this needs to be improved
- update timeSignature_ method: should stretch or compress leaf durations
- add extend(duration) method: extends timeSignature by duration, does not stretch leaves
- add rest (or note/chord) to beginning, end, or some other indexed part of measure without stretching leaves
--------------------------------------------------------------------------------------------------------------- */
LP_Measure : LP_FixedDurationContainer {
	var <timeSignature;
	var <indicatorsAtHead, <indicatorsAtTail, <commands, <overrides; //!!! move up to LP_Object and inherit
	var <parentVoice;
	*new { |timeSignature, children, notes|
		if (timeSignature.isArray) { timeSignature = LP_TimeSignature(*timeSignature) };
		^super.new(timeSignature.duration, children).timeSignature_(timeSignature).notes_(notes);
	}
	timeSignature_ { |argTimeSignature|
		if (argTimeSignature.isArray) { argTimeSignature = LP_TimeSignature(*argTimeSignature) };
		timeSignature = argTimeSignature;
		duration = timeSignature.duration;
	}
	//!!! move up to LP_Object and inherit
	// indicators only - LP_Markups and LP_Spanners can not be attached to a LP_Measure
	attach { |attachment|
		this.attachToHead(attachment);

	}
	attachToHead { |attachment|
		if (attachment.isKindOf(LP_Indicator)) {
			if (indicatorsAtHead.isNil) { indicatorsAtHead = OrderedIdentitySet[] };
			indicatorsAtHead = indicatorsAtHead.add(attachment);
		}
	}
	attachToTail { |attachment|
		if (attachment.isKindOf(LP_Indicator)) {
			if (indicatorsAtTail.isNil) { indicatorsAtTail = OrderedIdentitySet[] };
			indicatorsAtTail = indicatorsAtTail.add(attachment);
		}
	}
	//!!! move up to LP_Object and inherit
	override { |property, value|
		if (overrides.isNil) { overrides = OrderedIdentitySet[] };
		overrides = overrides.add(property -> value);
	}
	//!!! move up to LP_Object and inherit
	addCommand { |command|
		if (commands.isNil) { commands = OrderedIdentitySet[] };
		commands = commands.add(command);
	}
	//!!!
	addParent { |parent|
		parentVoice = parent;
	}
	nextMeasure {
		var measures, node;
		^if (parentVoice.notNil) {
			measures = parentVoice.measures.as(LinkedList);
			node = measures.findNodeOfObj(this).next;
			if (node.notNil) { node.obj }
		};
	}
	// get the previous measure in this voice
	prevMeasure {
		var measures, node;
		^if (parentVoice.notNil) {
			measures = parentVoice.measures.as(LinkedList);
			node = measures.findNodeOfObj(this).prev;
			if (node.notNil) { node.obj };
		};
	}

	rewriteTuplets {
		this.do { |node| if (node.isKindOf(LP_FixedDurationContainer)) { node.rewrite } };
	}
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_Tuplet

x = LP_Measure([3, 4], [LP_Tuplet(3, 1!5)]);
x[0].tupletRatio_(1);
LP_File(LP_Score([LP_Staff([x])])).write;
--------------------------------------------------------------------------------------------------------------- */
LP_Tuplet : LP_FixedDurationContainer {
	isTrivial {
		^(isTuplet.not || (this.leaves.size == 1));
	}
	// force tuplet ratio
	tupletRatio_ {
	}
	// replace trivial tuplets with their children
	// durations of the children are rescaled to sum to the duration of the parent
	/*rewrite {
		var rescale, isRewritable;
		if (this.isTrivial)  {
			if (this.leaves.size == 1) {
				//!!! tidy this
				if (children[0].isKindOf(LP_TieContainer)) {
					this.parent.replace(this, children[0][0].preProlatedDuration_(this.preProlatedDuration));
				} {
					this.parent.replace(this, children[0].preProlatedDuration_(this.preProlatedDuration));
				};
			} {
				rescale = children.collect { |each| each.preProlatedDuration }.normalizeSum * preProlatedDuration;
				rescale = (rescale.size / rescale.sum).asInteger;
				this.root.leaves.do { |leaf| leaf.preProlatedDuration_(leaf.preProlatedDuration * rescale) };
				isRewritable = this.nodes.collect { |node| node.preProlatedDuration }.includes(0).not;
				if (isRewritable) { this.parent.replaceAll(this, children) };
			};
		};
	}*/
}
/* ---------------------------------------------------------------------------------------------------------------
• LP_TieContainer
- invisible to clients, behaves like a leaf

a = LP_Measure([4, 4], [1, -1, 5, 1], [62, [60, 65], 64]);
a[2].children.do { |e| e.noteHeads[0].color_(Color.black).style_(\cross) };

a[2].noteHeads[0].color_(Color.black).style_(\cross);
a[2].noteHeads[1].style_(\harmonic);
a[2].attach(LP_Articulation('>'));
LP_File(LP_Score([LP_Staff([a])])).write(openPDF: true);

- interfaces for LP_Note and LP_Chord must also be implemented for LP_TieContainer
LP_Note:
note (  )
noteName (  )
noteHead (  )
dynamic (  )
note_ ( argNote )
noteName_ ( noteName )

LP_Chord:
notes (  )
noteNames (  )
noteHeads (  )
dynamic (  )
tweaks (  )
notes_ ( argNotes )
noteNames_ ( noteNames )

a = LP_Measure([4, 4], [1, -1, 5, 1], [62, 60]);
a[2].noteHeads;
--------------------------------------------------------------------------------------------------------------- */
LP_TieContainer : LP_FixedDurationContainer {
	var <>isTiedToNext=false;
	*new { |leaf|
		var preProlatedDurations, children;
		preProlatedDurations = LP_Duration.partitionNum(leaf.preProlatedDuration); //!!! update - use new method on Int
		children = preProlatedDurations.collect { |dur| leaf.clone.preProlatedDuration_(dur) };
		children.drop(-1).do { |child| child.isTiedToNext_(true) };
		^super.new(leaf.preProlatedDuration, children).isTiedToNext_(leaf.isTiedToNext);
	}
	clone {
		^children[0].clone.preProlatedDuration_(preProlatedDuration);
	}
	shallowClone {
		^children[0].shallowClone.preProlatedDuration_(preProlatedDuration);
	}
	attach { |attachment|
		if (attachment.isKindOf(LP_Tie)) {
			children.last.attach(attachment);
		} { children.first.attach(attachment) };
	}
	beatDuration {
		var prolation;
		prolation = this.parents.collect { |parent| parent.prolation }.reduce('*');
		^duration.beatDuration * prolation;
	}
	type {
		^children[0].class;
	}
}